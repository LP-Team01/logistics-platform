package com.logistics.delivery.command.dto.response;

import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.RouteRecordStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UpdateDeliveryRouteRecordResponseDto(
    UUID routeRecordId,
    Integer sequence,
    RouteRecordStatus status,
    Integer actualDistance,
    Integer actualDuration,
    Instant updatedAt
) {
    public static UpdateDeliveryRouteRecordResponseDto from(DeliveryRouteRecord routeRecord) {
        return UpdateDeliveryRouteRecordResponseDto.builder()
            .routeRecordId(routeRecord.getId())
            .sequence(routeRecord.getSequence())
            .status(routeRecord.getStatus())
            .actualDistance(routeRecord.getActualDistance())
            .actualDuration(routeRecord.getActualDuration())
            .updatedAt(routeRecord.getUpdatedAt())
            .build();
    }
}
