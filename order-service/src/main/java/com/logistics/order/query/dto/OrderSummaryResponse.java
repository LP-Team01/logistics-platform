package com.logistics.order.query.dto;

import com.logistics.order.domain.entity.OrderStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * 주문 목록 한 줄에 표시할 데이터입니다.
 */
public record OrderSummaryResponse(
        UUID orderId,
        UUID receiverCompanyId,
        OrderStatus status,
        Long totalAmount,
        Instant createdAt
) {
}
