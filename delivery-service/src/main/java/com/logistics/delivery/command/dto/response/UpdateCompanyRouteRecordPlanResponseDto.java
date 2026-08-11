package com.logistics.delivery.command.dto.response;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "업체 배송 경로 계획 수동 보정 결과")
public record UpdateCompanyRouteRecordPlanResponseDto(
    @Schema(description = "업체 배송 경로 기록 id")
    UUID recordId,
    @Schema(description = "수령 업체 위도")
    Double latitude,
    @Schema(description = "수령 업체 경도")
    Double longitude,
    @Schema(description = "예상 이동 거리(m)")
    Integer estimatedDistance,
    @Schema(description = "예상 소요 시간(분)")
    Integer estimatedDuration,
    @Schema(description = "방문 순서")
    Integer deliverySequence,
    @Schema(description = "수정 일시")
    Instant updatedAt
) {
    public static UpdateCompanyRouteRecordPlanResponseDto from(CompanyDeliveryRouteRecord routeRecord) {
        return UpdateCompanyRouteRecordPlanResponseDto.builder()
            .recordId(routeRecord.getId())
            .latitude(routeRecord.getLatitude())
            .longitude(routeRecord.getLongitude())
            .estimatedDistance(routeRecord.getEstimatedDistance())
            .estimatedDuration(routeRecord.getEstimatedDuration())
            .deliverySequence(routeRecord.getDeliverySequence())
            .updatedAt(routeRecord.getUpdatedAt())
            .build();
    }
}