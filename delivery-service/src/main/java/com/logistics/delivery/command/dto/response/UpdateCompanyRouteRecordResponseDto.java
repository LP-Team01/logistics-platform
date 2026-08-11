package com.logistics.delivery.command.dto.response;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "업체 배송 경로 상태 변경 결과")
public record UpdateCompanyRouteRecordResponseDto(
    @Schema(description = "업체 배송 경로 기록 id")
    UUID recordId,
    @Schema(description = "변경된 업체 배송 경로 상태")
    CompanyRouteRecordStatus status,
    @Schema(description = "실제 이동 거리(m)")
    Integer actualDistance,
    @Schema(description = "실제 소요 시간(분)")
    Integer actualDuration,
    @Schema(description = "수정 일시")
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
