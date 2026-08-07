package com.logistics.delivery.infrastructure.client.dto;

import java.util.List;
import java.util.UUID;

// delivery-service가 1차 NN으로 이미 순서를 매긴 결과를 ai-notification-service에 넘겨 미세조정을 요청
public record VisitSequenceRefinementRequestDto(
    UUID agentId,
    UUID hubId,
    Double hubLatitude,
    Double hubLongitude,
    List<Stop> stops
) {
    // stops는 1차 NN 순서 그대로 전달 (sequence 오름차순)
    public record Stop(
        UUID recordId,
        UUID receiverCompanyId,
        Double latitude,
        Double longitude,
        Integer estimatedDistance,
        Integer estimatedDuration,
        Integer sequence
    ) {
    }
}
