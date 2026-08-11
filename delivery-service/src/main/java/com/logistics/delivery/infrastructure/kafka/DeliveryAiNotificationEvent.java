package com.logistics.delivery.infrastructure.kafka;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


//delivery-service가 ai-notification-service로 발행하는 Kafka 이벤트
public record DeliveryAiNotificationEvent(
    UUID eventId,
    String eventVersion,
    DeliveryAiNotificationEventType eventType,
    Instant occurredAt,
    UUID orderId,
    UUID deliveryId,
    UUID recipientUserId,
    String recipientSlackId,
    List<ProductItem> products,
    String requestText,
    String departureLocation,
    List<String> waypointLocations,
    String destinationLocation,
    LocalDateTime requestedArrivalAt,
    Integer estimatedDurationMinutes,
    Integer preparationBufferMinutes
) {

    public record ProductItem(
        String productName,
        Integer quantity
    ) {
    }
}
