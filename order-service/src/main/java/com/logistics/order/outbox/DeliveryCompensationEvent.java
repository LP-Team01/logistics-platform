package com.logistics.order.outbox;

import java.time.Instant;
import java.util.UUID;

public record DeliveryCompensationEvent(
        UUID eventId,
        UUID orderId,
        Instant occurredAt
) {
}
