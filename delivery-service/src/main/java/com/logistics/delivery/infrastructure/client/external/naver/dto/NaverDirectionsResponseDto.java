package com.logistics.delivery.infrastructure.client.external.naver.dto;

import java.util.List;
import java.util.Map;

public record NaverDirectionsResponseDto(
    Integer code,
    String message,
    Map<String, List<Route>> route
) {
    public record Route(Summary summary) {
    }

    // distance 단위 m, duration 단위 ms
    public record Summary(Long distance, Long duration) {
    }
}
