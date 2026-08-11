package com.logistics.user.user.service;

import com.logistics.user.global.exception.BusinessException;
import com.logistics.user.global.exception.ErrorCode;
import com.logistics.user.global.exception.FeignExceptionTranslator;
import com.logistics.user.kafka.DeliveryManagerApprovalRequestedEvent;
import com.logistics.user.kafka.KafkaService;
import com.logistics.user.user.client.CompanyClient;
import com.logistics.user.user.client.CompanyResponseDto;
import com.logistics.user.user.client.HubClient;
import com.logistics.user.user.dto.request.UpdateRequestDto;
import com.logistics.user.user.dto.request.UserRequestDto;
import com.logistics.user.user.dto.request.UserSearchCondition;
import com.logistics.user.user.dto.response.UserResponseDto;
import com.logistics.user.user.dto.response.UserStatusResponse;
import com.logistics.user.user.dto.response.UserUpdateResponseDto;
import com.logistics.user.user.entity.AgentType;
import com.logistics.user.user.entity.User;
import com.logistics.user.user.entity.UserRole;
import com.logistics.user.user.entity.UserStatus;
import com.logistics.user.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyClient companyClient;
    private final HubClient hubClient;
    private final KafkaService kafkaService;
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 30, 50);
    private static final int DEFAULT_PAGE_SIZE = 10;


    @Transactional
    public UserResponseDto signUp(UserRequestDto requestDto) {
        // 닉네임 중복확인
        validateDuplicateId(requestDto.username());
        // slackID 중복확인
        validateDuplicateSlackId(requestDto.slackId());
        // 권한별 소속(hubId/companyId) 조합 검증
        validateAffiliation(requestDto.role(), requestDto.hubId(), requestDto.companyId());

        switch (requestDto.role()){
            case HUB_MANAGER -> checkHubId(requestDto.hubId());
            case COMPANY_MANAGER -> validateCompanyBelongsToHub(requestDto.companyId(), requestDto.hubId());
            case DELIVERY_MANAGER -> {
                if(requestDto.hubId() != null) checkHubId(requestDto.hubId());
            }
        }

        User user = User.builder()
            .username(requestDto.username())
            .encodedPassword(passwordEncoder.encode(requestDto.password()))
            .slackId(requestDto.slackId())
            .role(requestDto.role())
            .status(UserStatus.PENDING)
            .hubId(requestDto.hubId())
            .companyId(requestDto.companyId())
            .build();

        User savedUser = userRepository.save(user);
        return UserResponseDto.from(savedUser);
    }

    @Cacheable(value = "users", key = "#userId")
    public UserResponseDto getUser(UUID userId) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow(
            () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );
        return UserResponseDto.from(user);
    }
    public Page<UserResponseDto> getPendingUserByHub(UUID hubId, UUID requesterHubId, Pageable pageable) {
        if(!hubId.equals(requesterHubId)){
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return userRepository.findAllByHubIdAndDeletedAtIsNullAndStatus(hubId,UserStatus.PENDING, pageable)
            .map(UserResponseDto::from);
    }
    public Page<UserResponseDto> searchUsers(UserSearchCondition condition, Pageable pageable) {
        Pageable resolved = resolvePageable(pageable);
        return userRepository.search(condition, resolved).map(UserResponseDto::from);
    }
    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    public UserUpdateResponseDto updateUser(UUID userId, UpdateRequestDto requestDto) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow(
            () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );

        validateUpdate(user, requestDto);

        user.update(requestDto);

        return UserUpdateResponseDto.from(user);
    }
    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    public void deleteUser(UUID userId, UUID deletedBy) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow(
            () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );

        user.softDelete(deletedBy);
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    public UserStatusResponse rejectUser(UUID userId) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow(
            () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );

        if(user.getStatus() != UserStatus.PENDING){
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED_SIGNUP);
        }

        user.reject();

        return UserStatusResponse.from(user);
    }


    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    public UserStatusResponse approvedUser(UUID userId, UUID requesterId, UserRole managerRole, UUID managerHubId) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow(
            () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );

        if(user.getStatus() != UserStatus.PENDING){
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED_SIGNUP);
        }
        AgentType agentType = user.getHubId() == null ? AgentType.HUB_DELIVERY : AgentType.COMPANY_DELIVERY;
        validateApprovalScope(managerRole, managerHubId, user, agentType);

        log.info("유저 승인 처리. userId={}, requesterId={}, managerRole={}", userId, requesterId, managerRole);

        if (user.getRole() == UserRole.DELIVERY_MANAGER) {
            user.approving();

            DeliveryManagerApprovalRequestedEvent event = new DeliveryManagerApprovalRequestedEvent(
                UUID.randomUUID(),
                user.getUserId(),
                user.getHubId(),
                agentType,
                user.getSlackId(),
                requesterId,
                Instant.now()
            );
            // 커밋 전에 발행하면 delivery-service의 콜백(GET /api/users/{id})이 아직 반영 안 된 상태를 볼 수 있어
            // 커밋 이후로 발행을 미룬다
            runAfterCommit(() -> kafkaService.approvalDeliveryAgent(String.valueOf(user.getUserId()), event));
        }else{
            user.approve();
        }

        return UserStatusResponse.from(user);
    }

    private void runAfterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    // MASTER는 전부 승인 가능. HUB_MANAGER는 COMPANY_MANAGER와 COMPANY_DELIVERY 타입 DELIVERY_MANAGER를
    // 자기 담당 허브(managerHubId == 대상의 hubId) 한해서만 승인 가능.
    private void validateApprovalScope(UserRole managerRole, UUID managerHubId, User target, AgentType agentType) {
        if (managerRole == UserRole.MASTER) {
            return;
        }
        if (managerRole != UserRole.HUB_MANAGER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        boolean allowedTargetRole = target.getRole() == UserRole.COMPANY_MANAGER
            || (target.getRole() == UserRole.DELIVERY_MANAGER && agentType == AgentType.COMPANY_DELIVERY);
        if (!allowedTargetRole) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (!Objects.equals(managerHubId, target.getHubId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }


    private void validateDuplicateSlackId(String slackId) {
        if(userRepository.existsBySlackIdAndDeletedAtIsNullAndStatusNot(slackId, UserStatus.REJECTED)){
            throw new BusinessException(ErrorCode.DUPLICATE_SLACK_ID);
        }
    }

    private void validateDuplicateId(String username) {
        if(userRepository.existsByUsernameAndDeletedAtIsNullAndStatusNot(username, UserStatus.REJECTED)){
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
    }


    private void validateAffiliation(UserRole role, UUID hubId, UUID companyId) {
        switch (role) {
            case HUB_MANAGER -> {
                if(hubId == null || companyId != null){
                    throw new BusinessException(ErrorCode.BAD_REQUEST);
                }
            }
            case COMPANY_MANAGER -> {
                if(companyId == null || hubId == null){
                    throw new BusinessException(ErrorCode.BAD_REQUEST);
                }
            }
            case DELIVERY_MANAGER -> {
                if(companyId != null){
                    throw new BusinessException(ErrorCode.BAD_REQUEST);
                }
            }

        }
    }

    private Pageable resolvePageable(Pageable pageable){
        int size = ALLOWED_PAGE_SIZES.contains(pageable.getPageSize()) ?
            pageable.getPageSize() : DEFAULT_PAGE_SIZE;

        return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
    }


    private void validateUpdate(User user, UpdateRequestDto requestDto) {
        if (requestDto.username() != null && !requestDto.username().equals(user.getUsername())) {
            validateDuplicateId(requestDto.username());
        }
        if (requestDto.slackId() != null && !requestDto.slackId().equals(user.getSlackId())) {
            validateDuplicateSlackId(requestDto.slackId());
        }

        if (requestDto.role() != null || requestDto.hubId() != null || requestDto.companyId() != null) {
            UserRole resolvedRole = requestDto.role() != null ? requestDto.role() : user.getRole();
            UUID resolvedHubId = requestDto.hubId() != null ? requestDto.hubId() : user.getHubId();
            UUID resolvedCompanyId = requestDto.companyId() != null ? requestDto.companyId() : user.getCompanyId();

            validateAffiliation(resolvedRole, resolvedHubId, resolvedCompanyId);
            switch (resolvedRole) {
                case HUB_MANAGER -> checkHubId(resolvedHubId);
                case COMPANY_MANAGER -> validateCompanyBelongsToHub(resolvedCompanyId, resolvedHubId);
            }
        }
    }

    private void validateCompanyBelongsToHub(UUID companyId, UUID hubId) {
        CompanyResponseDto company = FeignExceptionTranslator.call(
            () -> companyClient.getCompany(companyId), ErrorCode.COMPANY_NOT_FOUND);
        if (!hubId.equals(company.hubId())) {
            throw new BusinessException(ErrorCode.HUB_COMPANY_MISMATCH);
        }
    }

    private void checkHubId(UUID hubId) {
        FeignExceptionTranslator.call(() -> hubClient.getHub(hubId), ErrorCode.HUB_NOT_FOUND);
    }


}
