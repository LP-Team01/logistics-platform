package com.logistics.delivery.command.dto.command;

import com.logistics.delivery.domain.entity.DeliveryStatus;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UpdateDeliveryCommand(
   DeliveryStatus status
) {
}
