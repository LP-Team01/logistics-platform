package com.logistics.order.query.dto;

import com.logistics.order.domain.entity.OrderItem;
import com.logistics.order.domain.entity.OrderItemStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * 주문 상세 조회에서 상품 한 개를 표현하는 응답입니다.
 */
public record OrderItemResponse(
        UUID orderItemId,
        UUID productId,
        String productName,
        Long unitPrice,
        Integer quantity,
        UUID supplierCompanyId,
        UUID deliveryId,
        OrderItemStatus status,
        Long subtotal,
        Instant requestedDeadline
) {

    public static OrderItemResponse from(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getOrderItemId(),
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity(),
                orderItem.getSupplierCompanyId(),
                orderItem.getDeliveryId(),
                orderItem.getStatus(),
                orderItem.getSubtotal(),
                orderItem.getRequestedDeadline()
        );
    }
}
