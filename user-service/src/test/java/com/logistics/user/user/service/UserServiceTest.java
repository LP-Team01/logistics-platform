package com.logistics.user.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.logistics.user.global.exception.BusinessException;
import com.logistics.user.global.exception.ErrorCode;
import com.logistics.user.kafka.DeliveryManagerApprovalRequestedEvent;
import com.logistics.user.kafka.outbox.DeliveryManagerApprovalOutboxService;
import com.logistics.user.user.client.CompanyClient;
import com.logistics.user.user.client.CompanyResponseDto;
import com.logistics.user.user.client.HubClient;
import com.logistics.user.user.client.HubResponseDto;
import com.logistics.user.user.dto.request.UserRequestDto;
import com.logistics.user.user.entity.User;
import com.logistics.user.user.entity.UserRole;
import com.logistics.user.user.entity.UserStatus;
import com.logistics.user.user.repository.UserRepository;
import feign.FeignException;
import feign.Request;
import feign.Request.HttpMethod;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CompanyClient companyClient;
    @Mock
    private HubClient hubClient;
    @Mock
    private DeliveryManagerApprovalOutboxService deliveryManagerApprovalOutboxService;

    @InjectMocks
    private UserService userService;

    private UUID hubId;
    private UUID companyId;
    private UUID userId;
    private UUID requesterId;

    @BeforeEach
    void setUp() {
        hubId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        requesterId = UUID.randomUUID();
    }

    @Nested
    class SignUp {

        @Test
        void hubManagerSignUpValidatesHubExists() {
            // given
            UserRequestDto requestDto = hubManagerRequest(hubId);
            when(userRepository.existsByUsernameAndDeletedAtIsNullAndStatusNot(any(), any())).thenReturn(false);
            when(userRepository.existsBySlackIdAndDeletedAtIsNullAndStatusNot(any(), any())).thenReturn(false);
            when(hubClient.getHub(hubId)).thenReturn(hubResponse(hubId));
            when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when & then
            assertThatCode(() -> userService.signUp(requestDto)).doesNotThrowAnyException();
        }

        @Test
        void hubManagerSignUpFailsWhenHubDoesNotExist() {
            // given
            UserRequestDto requestDto = hubManagerRequest(hubId);
            when(userRepository.existsByUsernameAndDeletedAtIsNullAndStatusNot(any(), any())).thenReturn(false);
            when(userRepository.existsBySlackIdAndDeletedAtIsNullAndStatusNot(any(), any())).thenReturn(false);
            when(hubClient.getHub(hubId)).thenThrow(notFound());

            // when & then
            assertThatThrownBy(() -> userService.signUp(requestDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.HUB_NOT_FOUND);
        }

        @Test
        void companyManagerSignUpFailsWhenCompanyBelongsToDifferentHub() {
            // given
            UUID otherHubId = UUID.randomUUID();
            UserRequestDto requestDto = companyManagerRequest(hubId, companyId);
            when(userRepository.existsByUsernameAndDeletedAtIsNullAndStatusNot(any(), any())).thenReturn(false);
            when(userRepository.existsBySlackIdAndDeletedAtIsNullAndStatusNot(any(), any())).thenReturn(false);
            when(companyClient.getCompany(companyId)).thenReturn(companyResponse(companyId, otherHubId));

            // when & then
            assertThatThrownBy(() -> userService.signUp(requestDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.HUB_COMPANY_MISMATCH);
        }

        @Test
        void deliveryManagerSignUpWithoutHubIdSkipsHubCheck() {
            // given
            UserRequestDto requestDto = deliveryManagerRequest(null);
            when(userRepository.existsByUsernameAndDeletedAtIsNullAndStatusNot(any(), any())).thenReturn(false);
            when(userRepository.existsBySlackIdAndDeletedAtIsNullAndStatusNot(any(), any())).thenReturn(false);
            when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when & then
            assertThatCode(() -> userService.signUp(requestDto)).doesNotThrowAnyException();
            verifyNoInteractions(hubClient);
        }

        @Test
        void duplicateUsernameFails() {
            // given
            UserRequestDto requestDto = hubManagerRequest(hubId);
            when(userRepository.existsByUsernameAndDeletedAtIsNullAndStatusNot(any(), any())).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signUp(requestDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_USERNAME);
        }
    }

    @Nested
    class ValidateApprovalScope {
        // approvedUser()를 통해 간접적으로 검증 (validateApprovalScope는 private)

        @Test
        void masterCanApproveAnyone() {
            // given: target은 다른 허브 소속 DELIVERY_MANAGER(COMPANY_DELIVERY) - hub 매칭 없이도 통과해야 함
            UUID otherHubId = UUID.randomUUID();
            User target = pendingUser(UserRole.DELIVERY_MANAGER, otherHubId);
            when(userRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(target));

            // when & then
            assertThatCode(() ->
                userService.approvedUser(userId, requesterId, UserRole.MASTER, hubId))
                .doesNotThrowAnyException();
            assertThat(target.getStatus()).isEqualTo(UserStatus.APPROVING);
            verify(deliveryManagerApprovalOutboxService).save(any(DeliveryManagerApprovalRequestedEvent.class));
        }

        @Test
        void hubManagerCanApproveCompanyManagerWithinOwnHub() {
            // given
            User target = pendingUser(UserRole.COMPANY_MANAGER, hubId);
            when(userRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(target));

            // when & then
            assertThatCode(() ->
                userService.approvedUser(userId, requesterId, UserRole.HUB_MANAGER, hubId))
                .doesNotThrowAnyException();
            assertThat(target.getStatus()).isEqualTo(UserStatus.APPROVED);
        }

        @Test
        void hubManagerCanApproveCompanyDeliveryWithinOwnHub() {
            // given: hubId가 있는 DELIVERY_MANAGER == COMPANY_DELIVERY
            User target = pendingUser(UserRole.DELIVERY_MANAGER, hubId);
            when(userRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(target));

            // when & then
            assertThatCode(() ->
                userService.approvedUser(userId, requesterId, UserRole.HUB_MANAGER, hubId))
                .doesNotThrowAnyException();
            assertThat(target.getStatus()).isEqualTo(UserStatus.APPROVING);
            verify(deliveryManagerApprovalOutboxService).save(any(DeliveryManagerApprovalRequestedEvent.class));
        }

        @Test
        void hubManagerCannotApproveHubDeliveryEvenInOwnHub() {
            // given: hubId가 없는 DELIVERY_MANAGER == HUB_DELIVERY, HUB_MANAGER는 승인 불가
            User target = pendingUser(UserRole.DELIVERY_MANAGER, null);
            when(userRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(target));

            // when & then
            assertThatThrownBy(() ->
                userService.approvedUser(userId, requesterId, UserRole.HUB_MANAGER, hubId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        }

        @Test
        void hubManagerCannotApproveOutsideOwnHub() {
            // given
            UUID otherHubId = UUID.randomUUID();
            User target = pendingUser(UserRole.COMPANY_MANAGER, otherHubId);
            when(userRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(target));

            // when & then
            assertThatThrownBy(() ->
                userService.approvedUser(userId, requesterId, UserRole.HUB_MANAGER, hubId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        }

        @Test
        void deliveryManagerCannotApproveAnyone() {
            // given
            User target = pendingUser(UserRole.COMPANY_MANAGER, hubId);
            when(userRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(target));

            // when & then
            assertThatThrownBy(() ->
                userService.approvedUser(userId, requesterId, UserRole.DELIVERY_MANAGER, hubId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        }
    }

    @Nested
    class GetPendingUserByHub {

        @Test
        void throwsAccessDeniedWhenHubIdMismatch() {
            // given
            UUID otherHubId = UUID.randomUUID();

            // when & then
            assertThatThrownBy(() ->
                userService.getPendingUserByHub(hubId, otherHubId, org.springframework.data.domain.Pageable.unpaged()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        }
    }

    // ===== 테스트 픽스처 =====

    private UserRequestDto hubManagerRequest(UUID hubId) {
        return new UserRequestDto(
            "hubmanager1", "Password1!", "slack-id-1", UserRole.HUB_MANAGER, hubId, null
        );
    }

    private UserRequestDto companyManagerRequest(UUID hubId, UUID companyId) {
        return new UserRequestDto(
            "companymgr1", "Password1!", "slack-id-2", UserRole.COMPANY_MANAGER, hubId, companyId
        );
    }

    private UserRequestDto deliveryManagerRequest(UUID hubId) {
        return new UserRequestDto(
            "deliverymgr1", "Password1!", "slack-id-3", UserRole.DELIVERY_MANAGER, hubId, null
        );
    }

    private User pendingUser(UserRole role, UUID hubId) {
        return User.builder()
            .username("agent1")
            .encodedPassword("encoded-password")
            .slackId("slack-id-9")
            .role(role)
            .status(UserStatus.PENDING)
            .hubId(hubId)
            .companyId(null)
            .build();
    }

    private HubResponseDto hubResponse(UUID hubId) {
        return new HubResponseDto(hubId, "테스트허브", "서울시 어딘가", 37.5, 127.0,
            Instant.now(), UUID.randomUUID(), Instant.now(), UUID.randomUUID());
    }

    private CompanyResponseDto companyResponse(UUID companyId, UUID hubId) {
        return new CompanyResponseDto(companyId, hubId, "테스트업체", "SUPPLIER", "주소",
            LocalDateTime.now(), UUID.randomUUID().toString());
    }

    // Feign 404 예외를 그대로 만들 수는 없어서(생성자가 Request/Response를 요구함) 직접 조립
    private FeignException.NotFound notFound() {
        Request request = Request.create(HttpMethod.GET, "/api/hubs/{id}", java.util.Map.of(),
            null, StandardCharsets.UTF_8, null);
        Response response = Response.builder()
            .status(404)
            .reason("Not Found")
            .request(request)
            .body("{\"code\":\"HUB_404\"}", StandardCharsets.UTF_8)
            .build();
        return (FeignException.NotFound) FeignException.errorStatus("HubClient#getHub", response);
    }
}
