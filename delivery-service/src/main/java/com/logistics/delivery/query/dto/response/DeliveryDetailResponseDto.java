package com.logistics.delivery.query.dto.response;

import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.DeliveryStatus;
import com.logistics.delivery.query.dto.response.DeliveryRouteResponseDto.DeliverRouteSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DeliveryDetailResponseDto(
    UUID deliveryId,
    UUID orderId,
    UUID orderItemId,
    DeliveryStatus status,
    UUID departureHubId,
    UUID destinationHubId,
    String deliveryAddress,
    String receiver,
    String receiverSlackId,
    UUID companyAgentId,
    List<DeliverRouteSummary> routeRecords,
    Instant createdAt,
    Instant updatedAt
) {
    public static DeliveryDetailResponseDto from(Delivery delivery, List<DeliveryRouteRecord> routeRecords) {
        return DeliveryDetailResponseDto.builder()
            .deliveryId(delivery.getId())
            .orderId(delivery.getOrderId())
            .orderItemId(delivery.getOrderItemId())
            .status(delivery.getStatus())
            .departureHubId(delivery.getDepartureHubId())
            .destinationHubId(delivery.getDestinationHubId())
            .deliveryAddress(delivery.getDeliveryAddress())
            .receiver(delivery.getReceiver())
            .receiverSlackId(delivery.getReceiverSlackId())
            .companyAgentId(delivery.getCompanyAgentId())
            .routeRecords(routeRecords.stream().map(DeliverRouteSummary::from).toList())
            .createdAt(delivery.getCreatedAt())
            .updatedAt(delivery.getUpdatedAt())
            .build();
    }
}
