package com.logistics.delivery.query.dto.reponse;

import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.RouteRecordStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DeliveryRouteDetailResponseDto(
    UUID routeRecordId,
    UUID deliveryId,
    Integer sequence,
    UUID departureHubId,
    UUID arrivalHubId,
    Integer estimatedDistance,
    Integer estimatedDuration,
    Integer actualDistance,
    Integer actualDuration,
    RouteRecordStatus status,
    UUID agentId,
    Instant createdAt,
    Instant updatedAt
) {
    public static DeliveryRouteDetailResponseDto from(DeliveryRouteRecord routeRecord) {
        return DeliveryRouteDetailResponseDto.builder()
            .routeRecordId(routeRecord.getId())
            .deliveryId(routeRecord.getDeliveryId())
            .sequence(routeRecord.getSequence())
            .departureHubId(routeRecord.getDepartureHubId())
            .arrivalHubId(routeRecord.getArrivalHubId())
            .estimatedDistance(routeRecord.getEstimatedDistance())
            .estimatedDuration(routeRecord.getEstimatedDuration())
            .actualDistance(routeRecord.getActualDistance())
            .actualDuration(routeRecord.getActualDuration())
            .status(routeRecord.getStatus())
            .agentId(routeRecord.getAgentId())
            .createdAt(routeRecord.getCreatedAt())
            .updatedAt(routeRecord.getUpdatedAt())
            .build();
    }
}
