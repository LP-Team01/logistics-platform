package com.logistics.delivery.query.dto.reponse;

import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record DeliveryResponseDto(
    List<DeliverSummary> content,
    Long totalElements,
    Integer totalPages,
    Integer size,
    Integer number
) {

    public record DeliverSummary(
        UUID deliveryId,
        UUID orderId,
        UUID orderItemId,
        DeliveryStatus status,
        UUID departureHubId,
        UUID destinationHubId,
        String receiver,
        Instant createdAt
    ) {
        public static DeliverSummary from(Delivery delivery) {
            return new DeliverSummary(
                delivery.getId(),
                delivery.getOrderId(),
                delivery.getOrderItemId(),
                delivery.getStatus(),
                delivery.getDepartureHubId(),
                delivery.getDestinationHubId(),
                delivery.getReceiver(),
                delivery.getCreatedAt()
            );
        }
    }

    public static DeliveryResponseDto from(Page<Delivery> page) {
        return DeliveryResponseDto.builder()
            .content(page.getContent().stream().map(DeliverSummary::from).toList())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .size(page.getSize())
            .number(page.getNumber())
            .build();
    }
}
