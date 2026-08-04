package com.logistics.delivery.command.dto.command;

import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateDeliveryCommand(
    UUID orderId,
    UUID departureHubId,
    UUID destinationHubId,
    String deliveryAddress,
    String receiver,
    String receiverSlackId,
    UUID receiverCompanyId
) {
}
