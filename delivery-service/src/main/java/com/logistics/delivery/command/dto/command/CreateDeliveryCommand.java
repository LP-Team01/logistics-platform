package com.logistics.delivery.command.dto.command;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateDeliveryCommand(
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
