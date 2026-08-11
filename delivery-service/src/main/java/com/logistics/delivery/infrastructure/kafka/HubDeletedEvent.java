package com.logistics.delivery.infrastructure.kafka;

import java.time.Instant;
import java.util.UUID;


public record HubDeletedEvent(
    UUID hubId,
    Instant deletedAt,
    UUID deletedBy
) {
}
