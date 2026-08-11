package com.logistics.hub.hub.event;

import java.time.Instant;
import java.util.UUID;

public record HubDeletedEvent(
    UUID hubId,
    Instant deletedAt,
    UUID deletedBy

) {
}
