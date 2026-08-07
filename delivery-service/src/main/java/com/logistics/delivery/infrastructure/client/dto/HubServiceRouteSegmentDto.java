package com.logistics.delivery.infrastructure.client.dto;

import java.util.UUID;

public record HubServiceRouteSegmentDto(
    Integer sequence,
    UUID departureHubId,
    UUID arrivalHubId,
    Double distance,
    Integer duration
) {
}