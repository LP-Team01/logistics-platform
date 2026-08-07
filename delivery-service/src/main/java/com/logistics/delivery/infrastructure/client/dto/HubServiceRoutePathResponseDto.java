package com.logistics.delivery.infrastructure.client.dto;

import java.util.List;

public record HubServiceRoutePathResponseDto(
    Double totalDistance,
    Integer totalDuration,
    List<HubServiceRouteSegmentDto> path
) {
}