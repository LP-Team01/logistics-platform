package com.logistics.order.global.auth;

import com.logistics.order.domain.entity.Order;
import com.logistics.order.global.exception.BusinessException;
import com.logistics.order.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderOwnershipValidatorTest {

    private final OrderOwnershipValidator validator =
            new OrderOwnershipValidator();

    /** 마스터는 모든 주문 취소 가능 */
    @Test
    void masterCanCancelOrder() {
        Order order = createOrder(UUID.randomUUID());

        assertDoesNotThrow(() ->
                validator.validateCancelAccess(
                        order,
                        "MASTER",
                        null
                )
        );
    }

    /** 담당 허브 관리자는 주문 취소 가능 */
    @Test
    void assignedHubManagerCanCancelOrder() {
        UUID hubId = UUID.randomUUID();
        Order order = createOrder(hubId);

        assertDoesNotThrow(() ->
                validator.validateCancelAccess(
                        order,
                        "HUB_MANAGER",
                        hubId
                )
        );
    }

    /** 다른 허브 관리자는 주문 취소 불가 */
    @Test
    void otherHubManagerCannotCancelOrder() {
        Order order = createOrder(UUID.randomUUID());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validateCancelAccess(
                        order,
                        "HUB_MANAGER",
                        UUID.randomUUID()
                )
        );

        assertEquals(
                ErrorCode.ACCESS_DENIED,
                exception.getErrorCode()
        );
    }

    private Order createOrder(UUID hubId) {
        return Order.create(
                UUID.randomUUID(),
                hubId,
                "문 앞에 놓아주세요.",
                UUID.randomUUID()
        );
    }
}
