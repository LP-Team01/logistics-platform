package com.logistics.delivery.command.dto.request;

import com.logistics.delivery.command.dto.command.UpdateCompanyRouteRecordPlanCommand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

// Geocoding/Directions/방문순서 자동 계산이 실패했을 때 수동으로 보정하기 위한 요청 - 전부 선택값이지만 최소 하나는 있어야 함
public record UpdateCompanyRouteRecordPlanRequestDto(
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90 이하이어야 합니다.")
    Double latitude,

    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 180 이하이어야 합니다.")
    Double longitude,

    @Positive(message = "예상 거리는 양수여야 합니다.")
    Integer estimatedDistance,

    @Positive(message = "예상 소요 시간은 양수여야 합니다.")
    Integer estimatedDuration,

    @Positive(message = "방문 순서는 양수여야 합니다.")
    Integer deliverySequence
) {
    @AssertTrue(message = "적어도 하나의 필드는 값이 있어야 합니다.")
    public boolean isAnyFieldProvided() {
        return latitude != null || longitude != null || estimatedDistance != null
            || estimatedDuration != null || deliverySequence != null;
    }

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