package com.logistics.order.infrastructure.client.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * 배송 생성 요청
 */
public record CreateDeliveryRequest(
        UUID orderId,
        UUID orderItemId,
        UUID departureHubId,
        UUID destinationHubId,
        String deliveryAddress,
        String receiver,
        String receiverSlackId,
        UUID receiverCompanyId,
        UUID recipientUserId,
        String productName,
        Integer quantity,
        String requestText,
        Instant requestedArrivalAt
) {
}
