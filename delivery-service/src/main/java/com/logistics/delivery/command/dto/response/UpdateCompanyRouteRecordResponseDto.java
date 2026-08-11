package com.logistics.delivery.command.dto.response;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UpdateCompanyRouteRecordResponseDto(
    UUID recordId,
    CompanyRouteRecordStatus status,
    Integer actualDistance,
    Integer actualDuration,
    Instant updatedAt
) {
    public static UpdateCompanyRouteRecordResponseDto from(CompanyDeliveryRouteRecord routeRecord) {
        return UpdateCompanyRouteRecordResponseDto.builder()
            .recordId(routeRecord.getId())
            .status(routeRecord.getStatus())
            .actualDistance(routeRecord.getActualDistance())
            .actualDuration(routeRecord.getActualDuration())
            .updatedAt(routeRecord.getUpdatedAt())
            .build();
    }
}
