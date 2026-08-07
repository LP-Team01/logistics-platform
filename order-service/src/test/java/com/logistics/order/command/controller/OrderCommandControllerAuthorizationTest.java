package com.logistics.order.command.controller;

import com.logistics.order.command.application.OrderCommandService;
import com.logistics.order.global.auth.HeaderRoleValidator;
import com.logistics.order.global.auth.InternalServiceValidator;
import com.logistics.order.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = OrderCommandController.class,
        properties = "spring.cloud.config.enabled=false"
)
@Import({HeaderRoleValidator.class, GlobalExceptionHandler.class})
class OrderCommandControllerAuthorizationTest {

    private static final String CANCEL_BODY =
            "{\"cancelReason\":\"테스트 취소\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderCommandService orderCommandService;

    @MockitoBean
    private InternalServiceValidator internalServiceValidator;

    /** 업체 관리자는 주문 전체 취소 불가 */
    @Test
    void companyManagerCannotCancelOrder() throws Exception {
        mockMvc.perform(patch("/api/orders/{orderId}/cancel", UUID.randomUUID())
                        .header("X-User-Id", UUID.randomUUID())
                        .header("X-User-Role", "COMPANY_MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CANCEL_BODY))
                .andExpect(status().isForbidden());
    }

    /** 업체 관리자는 주문 상품 취소 불가 */
    @Test
    void companyManagerCannotCancelOrderItem() throws Exception {
        mockMvc.perform(patch(
                        "/api/orders/{orderId}/items/{orderItemId}/cancel",
                        UUID.randomUUID(),
                        UUID.randomUUID()
                )
                        .header("X-User-Id", UUID.randomUUID())
                        .header("X-User-Role", "COMPANY_MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CANCEL_BODY))
                .andExpect(status().isForbidden());
    }

    /** 마스터는 X-Company-Id 없이 전체 취소 요청 가능 */
    @Test
    void masterCanCancelWithoutCompanyId() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/orders/{orderId}/cancel", orderId)
                        .header("X-User-Id", userId)
                        .header("X-User-Role", "MASTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CANCEL_BODY))
                .andExpect(status().isOk());

        verify(orderCommandService).cancelOrder(
                eq(orderId),
                any(),
                eq(userId),
                eq("MASTER"),
                isNull()
        );
    }

    /** 허브 관리자는 X-Company-Id 없이 상품 취소 요청 가능 */
    @Test
    void hubManagerCanCancelWithoutCompanyId() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        mockMvc.perform(patch(
                        "/api/orders/{orderId}/items/{orderItemId}/cancel",
                        orderId,
                        orderItemId
                )
                        .header("X-User-Id", userId)
                        .header("X-User-Role", "HUB_MANAGER")
                        .header("X-Hub-Id", hubId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CANCEL_BODY))
                .andExpect(status().isOk());

        verify(orderCommandService).cancelOrderItem(
                eq(orderId),
                eq(orderItemId),
                any(),
                eq(userId),
                eq("HUB_MANAGER"),
                eq(hubId)
        );
    }
}
