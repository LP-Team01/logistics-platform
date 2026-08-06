package com.logistics.hub.hubroute.dto;

import com.logistics.hub.hubroute.entity.HubRoute;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record HubRouteResponseDto(
    UUID hubRouteId,
    UUID departureHubId,
    UUID arrivalHubId,
    Double distance,
    Integer duration,
    Instant createdAt,
    String createdBy,
    Instant updatedAt,
    String updatedBy
) implements Serializable {

    public static HubRouteResponseDto from(HubRoute hubRoute) {
        return new HubRouteResponseDto(
            hubRoute.getHubRouteId(),
            hubRoute.getDepartureHubId(),
            hubRoute.getArrivalHubId(),
            hubRoute.getDistance(),
            hubRoute.getDuration(),
            hubRoute.getCreatedAt(),
            hubRoute.getCreatedBy(),
            hubRoute.getUpdatedAt(),
            hubRoute.getUpdatedBy()
        );
    }
}
