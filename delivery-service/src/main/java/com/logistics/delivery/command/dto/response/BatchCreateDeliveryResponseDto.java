package com.logistics.delivery.command.dto.response;

import com.logistics.delivery.domain.entity.DeliveryStatus;
import java.util.UUID;

public record BatchCreateDeliveryResponseDto(
        UUID deliveryId,
        UUID orderItemId,
        DeliveryStatus status
) {
    public static BatchCreateDeliveryResponseDto from(CreateDeliveryResponseDto response) {
        return new BatchCreateDeliveryResponseDto(
                response.deliveryId(),
                response.orderItemId(),
                response.status()
        );
    }
}