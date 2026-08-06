package com.logistics.order.command.dto;

import com.logistics.order.domain.entity.Order;
import com.logistics.order.domain.entity.OrderStatus;

import java.util.UUID;

/**
 * 주문 생성이 성공했을 때 반환하는 최소 정보입니다.
 *
 * 상세 주문 내용은 Query API에서 별도로 조회합니다.
 */
public record OrderCommandResponse(
        UUID orderId,
        OrderStatus status,
        Long totalAmount
) {

    public static OrderCommandResponse from(Order order) {
        return new OrderCommandResponse(
                order.getOrderId(),
                order.getStatus(),
                order.getTotalAmount()
        );
    }
}
