package com.logistics.delivery.infrastructure.kafka;

import java.time.Instant;
import java.util.UUID;

public record DeliveryCompensationEvent(
        UUID eventId,
        UUID orderId,
        Instant occurredAt
) {
}
