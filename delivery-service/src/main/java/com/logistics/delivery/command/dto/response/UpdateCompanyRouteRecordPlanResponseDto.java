package com.logistics.delivery.command.dto.response;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UpdateCompanyRouteRecordPlanResponseDto(
    UUID recordId,
    Double latitude,
    Double longitude,
    Integer estimatedDistance,
    Integer estimatedDuration,
    Integer deliverySequence,
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