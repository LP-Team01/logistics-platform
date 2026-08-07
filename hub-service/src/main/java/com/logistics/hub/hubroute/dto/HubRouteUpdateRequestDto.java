package com.logistics.hub.hubroute.dto;

import jakarta.validation.constraints.Positive;

public record HubRouteUpdateRequestDto(

    @Positive(message = "이동 거리는 0보다 커야 합니다.")
    Double distance,

    @Positive(message = "소요 시간은 0보다 커야 합니다.")
    Integer duration
    ) {
}
