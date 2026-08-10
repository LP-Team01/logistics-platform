package com.logistics.hub.hubroute.dto;

import com.logistics.hub.hubroute.service.HubRoutePathService;

import java.util.ArrayList;
import java.util.List;

public record HubRoutePathResponseDto(
    double totalDistance,
    int totalDuration,
    List<PathSegmentDto> path
) {
    public static HubRoutePathResponseDto from(HubRoutePathService.PathResult result) {
        List<PathSegmentDto> path = new ArrayList<>();
        int sequence = 1;
        for (HubRoutePathService.PathSegment segment : result.segments()) {
            path.add(PathSegmentDto.from(segment, sequence));
            sequence++;
        }

        return new HubRoutePathResponseDto(result.totalDistance(), result.totalDuration(), path);
    }
}
