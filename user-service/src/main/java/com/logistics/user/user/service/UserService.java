package com.logistics.user.user.service;

import com.logistics.user.global.exception.BusinessException;
import com.logistics.user.global.exception.ErrorCode;
import com.logistics.user.user.dto.request.DeliveryAgentRequestDto;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
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

        // TODO: hub-service/company-service에 실제 hubId/companyId 존재 여부 검증

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
    public Page<UserResponseDto> getPendingUserByHub(UUID hubId, Pageable pageable) {

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

        if(requestDto.username() != null && !requestDto.username().equals(user.getUsername())){
            validateDuplicateId(requestDto.username());
        }
        if(requestDto.slackId() != null && !requestDto.slackId().equals(user.getSlackId())){
            validateDuplicateSlackId(requestDto.slackId());
        }

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
    public UserStatusResponse approvedUser(UUID userId, DeliveryAgentRequestDto requestDto) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow(
            () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );

        if(user.getStatus() != UserStatus.PENDING){
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED_SIGNUP);
        }

        if(user.getRole() == UserRole.DELIVERY_MANAGER){
            if(requestDto.agentType() == null){
                throw new BusinessException(ErrorCode.BAD_REQUEST);
            }
            // TODO: OpenFeign으로 Delivery agent 생성
            if(requestDto.agentType().equals(AgentType.COMPANY_DELIVERY)){
                if(requestDto.hubId() == null){
                    throw new BusinessException(ErrorCode.BAD_REQUEST);
                }
                // 성공

                // 실패
            }else{
                // 성공

                // 실패
            }
        }

        user.approve();

        return UserStatusResponse.from(user);
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

        }
    }

    private Pageable resolvePageable(Pageable pageable){
        int size = ALLOWED_PAGE_SIZES.contains(pageable.getPageSize()) ?
            pageable.getPageSize() : DEFAULT_PAGE_SIZE;

        return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
    }


}
