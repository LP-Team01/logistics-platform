package com.logistics.delivery.query.dto.response;

import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.RouteRecordStatus;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DeliveryRouteResponseDto(
    UUID deliveryId,
    List<DeliverRouteSummary> routeRecords
) {

    public record DeliverRouteSummary(
        UUID routeRecordId,
        Integer sequence,
        UUID departureHubId,
        UUID arrivalHubId,
        Integer estimatedDistance,
        Integer estimatedDuration,
        Integer actualDistance,
        Integer actualDuration,
        RouteRecordStatus status,
        UUID agentId
    ) {
        public static DeliverRouteSummary from(DeliveryRouteRecord deliveryRouteRecord) {
            return new DeliverRouteSummary(
                deliveryRouteRecord.getId(),
                deliveryRouteRecord.getSequence(),
                deliveryRouteRecord.getDepartureHubId(),
                deliveryRouteRecord.getArrivalHubId(),
                deliveryRouteRecord.getEstimatedDistance(),
                deliveryRouteRecord.getEstimatedDuration(),
                deliveryRouteRecord.getActualDistance(),
                deliveryRouteRecord.getActualDuration(),
                deliveryRouteRecord.getStatus(),
                deliveryRouteRecord.getAgentId()
            );
        }
    }

    public static DeliveryRouteResponseDto from(UUID deliveryId, List<DeliveryRouteRecord> routeRecords) {
        return DeliveryRouteResponseDto.builder()
            .deliveryId(deliveryId)
            .routeRecords(routeRecords.stream().map(DeliverRouteSummary::from).toList())
            .build();
    }
}
