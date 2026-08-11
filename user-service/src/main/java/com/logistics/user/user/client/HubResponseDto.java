package com.logistics.user.user.client;

import java.time.Instant;
import java.util.UUID;

public record HubResponseDto(
    UUID hubId,
    String name,
    String address,
    Double latitude,
    Double longitude,
    Instant createdAt,
    UUID createdBy,
    Instant updatedAt,
    UUID updatedBy
) {
}
