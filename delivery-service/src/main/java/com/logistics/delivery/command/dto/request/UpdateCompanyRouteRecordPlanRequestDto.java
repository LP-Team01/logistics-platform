package com.logistics.delivery.command.dto.request;

import com.logistics.delivery.command.dto.command.UpdateCompanyRouteRecordPlanCommand;

// Geocoding/Directions/방문순서 자동 계산이 실패했을 때 수동으로 보정하기 위한 요청 - 전부 선택값
public record UpdateCompanyRouteRecordPlanRequestDto(
    Double latitude,
    Double longitude,
    Integer estimatedDistance,
    Integer estimatedDuration,
    Integer deliverySequence
) {
    public UpdateCompanyRouteRecordPlanCommand toCommand() {
        return UpdateCompanyRouteRecordPlanCommand.builder()
            .latitude(latitude)
            .longitude(longitude)
            .estimatedDistance(estimatedDistance)
            .estimatedDuration(estimatedDuration)
            .deliverySequence(deliverySequence)
            .build();
    }
}