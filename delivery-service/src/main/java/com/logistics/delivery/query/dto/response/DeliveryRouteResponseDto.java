package com.logistics.delivery.query.dto.response;

import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.RouteRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "허브 경로 기록 목록 조회 결과")
public record DeliveryRouteResponseDto(
    @Schema(description = "배송 id")
    UUID deliveryId,
    @Schema(description = "허브 간 경로 기록 목록")
    List<DeliverRouteSummary> routeRecords
) {

    @Schema(description = "허브 간 경로 기록 요약")
    public record DeliverRouteSummary(
        @Schema(description = "경로 기록 id")
        UUID routeRecordId,
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
        UUID agentId
    ) {
        public static DeliverRouteSummary from(DeliveryRouteRecord deliveryRouteRecord) {
            return new DeliverRouteSummary(
                deliveryRouteRecord.getId(),
                deliveryRouteRecord.getSequence(),
                deliveryRouteRecord.getDepartureHubId(),
                deliveryRouteRecord.getArrivalHubId(),
                deliveryRouteRecord.getEstimatedDistance(),
                deliveryRouteRecord.getEstimatedDuration(),
                deliveryRouteRecord.getActualDistance(),
                deliveryRouteRecord.getActualDuration(),
                deliveryRouteRecord.getStatus(),
                deliveryRouteRecord.getAgentId()
            );
        }
    }

    public static DeliveryRouteResponseDto from(UUID deliveryId, List<DeliveryRouteRecord> routeRecords) {
        return DeliveryRouteResponseDto.builder()
            .deliveryId(deliveryId)
            .routeRecords(routeRecords.stream().map(DeliverRouteSummary::from).toList())
            .build();
    }
}
