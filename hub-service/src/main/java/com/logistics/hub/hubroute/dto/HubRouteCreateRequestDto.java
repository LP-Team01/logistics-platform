package com.logistics.hub.hubroute.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record HubRouteCreateRequestDto(
    @NotNull(message = "출발 허브는 필수입니다.")
    UUID departureHubId,

    @NotNull(message = "도착 허브는 필수입니다.")
    UUID arrivalHubId,

    @NotNull(message = "이동 거리는 필수입니다.")
    @Positive(message = "이동 거리는 0보다 커야 합니다.")
    Double distance,

    @NotNull(message = "소요 시간은 필수입니다.")
    @Positive(message = "소요 시간은 0보다 커야 합니다.")
    Integer duration
    ) {
}
