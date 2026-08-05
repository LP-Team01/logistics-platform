package com.logistics.order.query.dto;

import com.logistics.order.domain.entity.Order;
import com.logistics.order.domain.entity.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 주문 하나와 주문에 포함된 상품 목록을 반환합니다.
 */
public record OrderDetailResponse(
    UUID orderId,
    UUID receiverCompanyId,
    OrderStatus status,
    Long totalAmount,
    String deliveryRequest,
    UUID canceledBy,
    String cancelReason,
    Instant createdAt,
    List<OrderItemResponse> items
) {

    public static OrderDetailResponse from(Order order) {
        List<OrderItemResponse> items =
                order.getOrderItems()
                        .stream()
                        .map(OrderItemResponse::from)
                        .toList();

        return new OrderDetailResponse(
                order.getOrderId(),
                order.getReceiverCompanyId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getDeliveryRequest(),
                order.getCanceledBy(),
                order.getCancelReason(),
                order.getCreatedAt(),
                items
        );
    }
}
