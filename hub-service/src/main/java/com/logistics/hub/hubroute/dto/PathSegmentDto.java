package com.logistics.hub.hubroute.dto;

import com.logistics.hub.hubroute.service.HubRoutePathService;

import java.util.UUID;

public record PathSegmentDto(
    int sequence,
    UUID departureHubId,
    UUID arrivalHubId,
    double distance,
    int duration
) {
    public static PathSegmentDto from(HubRoutePathService.PathSegment segment, int sequence) {
        return new PathSegmentDto(
            sequence,
            segment.departureHubId(),
            segment.arrivalHubId(),
            segment.distance(),
            segment.duration()
        );
    }
}
