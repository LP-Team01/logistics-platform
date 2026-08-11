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
public record CreateDeliveryResponseDto(
        UUID deliveryId,
        UUID orderId,
        UUID orderItemId,
        DeliveryStatus status,
        List<RouteRecordSummary> routeRecords,
        Instant createdAt
) {
    public record RouteRecordSummary(
            Integer sequence,
            UUID departureHubId,
            UUID arrivalHubId,
            Integer estimatedDistance,
            Integer estimatedDuration,
            RouteRecordStatus status
    ) {
        public static RouteRecordSummary from(DeliveryRouteRecord routeRecord) {
            return new RouteRecordSummary(
                    routeRecord.getSequence(),
                    routeRecord.getDepartureHubId(),
                    routeRecord.getArrivalHubId(),
                    routeRecord.getEstimatedDistance(),
                    routeRecord.getEstimatedDuration(),
                    routeRecord.getStatus()
            );
        }
    }

    public static CreateDeliveryResponseDto from(Delivery delivery, List<DeliveryRouteRecord> routeRecords) {
        return CreateDeliveryResponseDto.builder()
                .deliveryId(delivery.getId())
                .orderId(delivery.getOrderId())
                .orderItemId(delivery.getOrderItemId())
                .status(delivery.getStatus())
                .routeRecords(routeRecords.stream().map(RouteRecordSummary::from).toList())
                .createdAt(delivery.getCreatedAt())
                .build();
    }
}
