package com.logistics.hub.hubroute.dto;

import com.logistics.hub.hubroute.entity.HubRoute;

import java.time.Instant;
import java.util.UUID;

public record HubRouteDeleteResponseDto(
    UUID hubRouteId,
    Instant deletedAt,
    UUID deletedBy
) {
    public static HubRouteDeleteResponseDto from(HubRoute hubRoute) {
        return new HubRouteDeleteResponseDto(
            hubRoute.getHubRouteId(),
            hubRoute.getDeletedAt(),
            hubRoute.getDeletedBy()
        );
    }
}
