package com.logistics.delivery.command.dto.response;

import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.RouteRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "허브 경로 기록 상태 변경 결과")
public record UpdateDeliveryRouteRecordResponseDto(
    @Schema(description = "경로 기록 id")
    UUID routeRecordId,
    @Schema(description = "경로 구간 순번")
    Integer sequence,
    @Schema(description = "변경된 구간 배송 상태")
    RouteRecordStatus status,
    @Schema(description = "실제 이동 거리(m)")
    Integer actualDistance,
    @Schema(description = "실제 소요 시간(분)")
    Integer actualDuration,
    @Schema(description = "수정 일시")
    Instant updatedAt
) {
    public static UpdateDeliveryRouteRecordResponseDto from(DeliveryRouteRecord routeRecord) {
        return UpdateDeliveryRouteRecordResponseDto.builder()
            .routeRecordId(routeRecord.getId())
            .sequence(routeRecord.getSequence())
            .status(routeRecord.getStatus())
            .actualDistance(routeRecord.getActualDistance())
            .actualDuration(routeRecord.getActualDuration())
            .updatedAt(routeRecord.getUpdatedAt())
            .build();
    }
}
