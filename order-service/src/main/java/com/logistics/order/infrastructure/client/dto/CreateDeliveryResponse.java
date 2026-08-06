package com.logistics.order.infrastructure.client.dto;

import java.util.UUID;

/**
 * 배송 생성 응답
 */
public record CreateDeliveryResponse(
        UUID deliveryId,
        String status
) {
}
