package com.logistics.hub.hub.dto;

import com.logistics.hub.hub.entity.Hub;

import java.time.Instant;
import java.util.UUID;

public record HubDeleteResponseDto(
    UUID hubId,
    Instant deletedAt,
    UUID deletedBy
) {
    public static HubDeleteResponseDto from(Hub hub) {
        return new HubDeleteResponseDto(
            hub.getHubId(),
            hub.getDeletedAt(),
            hub.getDeletedBy()
        );
    }
}
