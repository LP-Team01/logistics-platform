package com.logistics.delivery.query.dto.response;

import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.RouteRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "허브 경로 기록 단건 조회 결과")
public record DeliveryRouteDetailResponseDto(
    @Schema(description = "경로 기록 id")
    UUID routeRecordId,
    @Schema(description = "배송 id")
    UUID deliveryId,
    @Schema(description = "경로 구간 순번")
    Integer sequence,
    @Schema(description = "출발 허브 id")
    UUID departureHubId,
    @Schema(description = "도착 허브 id")
    UUID arrivalHubId,
    @Schema(description = "예상 이동 거리(m)")
    Integer estimatedDistance,
    @Schema(description = "예상 소요 시간(분)")
    Integer estimatedDuration,
    @Schema(description = "실제 이동 거리(m)")
    Integer actualDistance,
    @Schema(description = "실제 소요 시간(분)")
    Integer actualDuration,
    @Schema(description = "구간 배송 상태")
    RouteRecordStatus status,
    @Schema(description = "담당 배송 담당자 id")
    UUID agentId,
    @Schema(description = "생성 일시")
    Instant createdAt,
    @Schema(description = "수정 일시")
    Instant updatedAt
) {
    public static DeliveryRouteDetailResponseDto from(DeliveryRouteRecord routeRecord) {
        return DeliveryRouteDetailResponseDto.builder()
            .routeRecordId(routeRecord.getId())
            .deliveryId(routeRecord.getDeliveryId())
            .sequence(routeRecord.getSequence())
            .departureHubId(routeRecord.getDepartureHubId())
            .arrivalHubId(routeRecord.getArrivalHubId())
            .estimatedDistance(routeRecord.getEstimatedDistance())
            .estimatedDuration(routeRecord.getEstimatedDuration())
            .actualDistance(routeRecord.getActualDistance())
            .actualDuration(routeRecord.getActualDuration())
            .status(routeRecord.getStatus())
            .agentId(routeRecord.getAgentId())
            .createdAt(routeRecord.getCreatedAt())
            .updatedAt(routeRecord.getUpdatedAt())
            .build();
    }
}
