package com.logistics.user.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.user.global.exception.GlobalExceptionHandler;
import com.logistics.user.user.dto.response.UserStatusResponse;
import com.logistics.user.user.entity.UserStatus;
import com.logistics.user.user.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// spring-boot-starter-security가 클래스패스에 있으면 @WebMvcTest가 실제 SecurityConfig(permitAll)를
// 안 가져오고 스프링 기본 보안(전체 인증 요구)을 적용해버려서, 시큐리티 필터 자체를 꺼서 순수하게
// 컨트롤러/헤더 바인딩만 검증한다. (실제 인가는 게이트웨이가 담당하는 구조라 여기선 상관없음)
@WebMvcTest(
    controllers = UserController.class,
    properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    /** MASTER는 X-Hub-Id 헤더 없이도 승인 가능 (required=false 확인용) */
    @Test
    void masterCanApproveWithoutHubIdHeader() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(userService.approvedUser(eq(userId), eq(requesterId), any(), eq(null)))
            .thenReturn(new UserStatusResponse(userId, UserStatus.APPROVED, null, requesterId));

        mockMvc.perform(patch("/api/users/{userId}/approved", userId)
                .header("X-User-Id", requesterId)
                .header("X-User-Role", "MASTER"))
            .andExpect(status().isOk());
    }

    /** HUB_MANAGER가 승인할 땐 X-Hub-Id가 서비스로 정확히 전달되는지 */
    @Test
    void hubManagerApproveForwardsHubIdHeader() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        when(userService.approvedUser(eq(userId), eq(requesterId), any(), eq(hubId)))
            .thenReturn(new UserStatusResponse(userId, UserStatus.APPROVED, null, requesterId));

        mockMvc.perform(patch("/api/users/{userId}/approved", userId)
                .header("X-User-Id", requesterId)
                .header("X-User-Role", "HUB_MANAGER")
                .header("X-Hub-Id", hubId))
            .andExpect(status().isOk());

        verify(userService).approvedUser(userId, requesterId, com.logistics.user.user.entity.UserRole.HUB_MANAGER, hubId);
    }

    /** X-User-Role 헤더가 없으면 400 (MissingRequestHeaderException) */
    @Test
    void approveWithoutRoleHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/users/{userId}/approved", UUID.randomUUID())
                .header("X-User-Id", UUID.randomUUID()))
            .andExpect(status().isBadRequest());
    }

    /** GET /api/users/hubs/{hubId}/pending 라우팅이 /{userId}랑 안 겹치는지 (컨텍스트 로딩 자체로 검증됨) */
    @Test
    void pendingByHubRouteDoesNotCollideWithUserIdRoute() throws Exception {
        UUID hubId = UUID.randomUUID();

        // X-Hub-Id 헤더 없이 호출하면 400 (required 헤더 누락)
        mockMvc.perform(get("/api/users/hubs/{hubId}/pending", hubId))
            .andExpect(status().isBadRequest());

        // X-Hub-Id 헤더가 있으면 getUser(userId)가 아니라 getPendingUserByHub가 정상 호출됨
        when(userService.getPendingUserByHub(eq(hubId), eq(hubId), any(Pageable.class)))
            .thenReturn(Page.empty());

        mockMvc.perform(get("/api/users/hubs/{hubId}/pending", hubId)
                .header("X-Hub-Id", hubId))
            .andExpect(status().isOk());
    }

    @Test
    void rejectUserWorksWithoutRoleOrHubHeader() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userService.rejectUser(userId))
            .thenReturn(new UserStatusResponse(userId, UserStatus.REJECTED, null, null));

        mockMvc.perform(patch("/api/users/{userId}/rejected", userId))
            .andExpect(status().isOk());
    }
}
