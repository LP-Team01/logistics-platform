package com.logistics.hub.domain.dto;

import com.logistics.hub.domain.entity.Hub;

import java.time.LocalDateTime;
import java.util.UUID;

public record HubResponseDto (
    UUID hubId,
    String name,
    String address,
    Double latitude,
    Double longitude,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
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


