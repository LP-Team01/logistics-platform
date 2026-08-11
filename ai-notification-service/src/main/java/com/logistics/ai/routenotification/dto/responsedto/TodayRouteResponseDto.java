package com.logistics.ai.routenotification.dto.responsedto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 배송담당자의 당일 방문 경로 API 응답입니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TodayRouteResponseDto(

    UUID agentId,
    Integer totalDistance,
    Integer totalDuration,
    Instant routeComputedAt,
    List<Stop> stops

) {

    /**
     * 방문 순서에 포함된 개별 배송지 정보입니다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stop(

        UUID recordId,
        UUID deliveryId,
        UUID receiverCompanyId,
        Double latitude,
        Double longitude,
        Integer estimatedDistance,
        Integer estimatedDuration,
        Integer deliverySequence,
        String status

    ) {
    }
}
