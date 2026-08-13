package com.logistics.delivery.command.dto.response;

import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.DeliveryStatus;
import com.logistics.delivery.domain.entity.RouteRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "배송 생성 결과")
public record CreateDeliveryResponseDto(
        @Schema(description = "배송 id")
        UUID deliveryId,
        @Schema(description = "주문 id")
        UUID orderId,
        @Schema(description = "주문아이템 id")
        UUID orderItemId,
        @Schema(description = "배송 상태")
        DeliveryStatus status,
        @Schema(description = "생성된 허브 간 경로 기록 목록")
        List<RouteRecordSummary> routeRecords,
        @Schema(description = "생성 일시")
        Instant createdAt
) {
    @Schema(description = "허브 간 경로 기록 요약")
    public record RouteRecordSummary(
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
            @Schema(description = "구간 배송 상태")
            RouteRecordStatus status
    ) {
        public static RouteRecordSummary from(DeliveryRouteRecord routeRecord) {
            return new RouteRecordSummary(
                    routeRecord.getSequence(),
                    routeRecord.getDepartureHubId(),
                    routeRecord.getArrivalHubId(),
                    routeRecord.getEstimatedDistance(),
                    routeRecord.getEstimatedDuration(),
                    routeRecord.getStatus()
            );
        }
    }

    public static CreateDeliveryResponseDto from(Delivery delivery, List<DeliveryRouteRecord> routeRecords) {
        return CreateDeliveryResponseDto.builder()
                .deliveryId(delivery.getId())
                .orderId(delivery.getOrderId())
                .orderItemId(delivery.getOrderItemId())
                .status(delivery.getStatus())
                .routeRecords(routeRecords.stream().map(RouteRecordSummary::from).toList())
                .createdAt(delivery.getCreatedAt())
                .build();
    }
}
