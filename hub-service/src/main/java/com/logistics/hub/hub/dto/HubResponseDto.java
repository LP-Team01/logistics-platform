package com.logistics.hub.hub.dto;

import com.logistics.hub.hub.entity.Hub;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record HubResponseDto (
    UUID hubId,
    String name,
    String address,
    Double latitude,
    Double longitude,
    Instant createdAt,
    String createdBy,
    Instant updatedAt,
    String updatedBy
) {

    public static HubResponseDto from(Hub hub) {
        return new HubResponseDto(
            hub.getHubId(),
            hub.getName(),
            hub.getAddress(),
            hub.getLatitude(),
            hub.getLongitude(),
            hub.getCreatedAt(),
            hub.getCreatedBy(),
            hub.getUpdatedAt(),
            hub.getUpdatedBy()
        );
    }
}


