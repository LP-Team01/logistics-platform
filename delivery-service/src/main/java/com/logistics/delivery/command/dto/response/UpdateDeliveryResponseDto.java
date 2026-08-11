package com.logistics.delivery.command.dto.response;

import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.DeliveryStatus;
import com.logistics.delivery.domain.entity.RouteRecordStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UpdateDeliveryResponseDto(
        UUID deliveryId,
        DeliveryStatus status,
        Instant updatedAt
) {
    public static UpdateDeliveryResponseDto from(Delivery delivery) {
        return UpdateDeliveryResponseDto.builder()
                .deliveryId(delivery.getId())
                .status(delivery.getStatus())
                .updatedAt(delivery.getUpdatedAt())
                .build();
    }
}
