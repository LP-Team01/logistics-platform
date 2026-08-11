package com.logistics.delivery.query.dto.response;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

// 업체배송담당자의 당일 방문 계획 - "매일 아침 6시 발송" 트리거 담당 서비스가 조회
@Builder
@Schema(description = "업체 배송 담당자 당일 방문 계획")
public record TodayRouteResponseDto(
    @Schema(description = "배송 담당자 id")
    UUID agentId,
    @Schema(description = "전체 이동 거리(m)")
    Integer totalDistance,
    @Schema(description = "전체 소요 시간(분)")
    Integer totalDuration,
    @Schema(description = "경로 산출 일시")
    Instant routeComputedAt,
    @Schema(description = "방문지 목록")
    List<Stop> stops
) {
    @Builder
    @Schema(description = "방문지")
    public record Stop(
        @Schema(description = "업체 배송 경로 기록 id")
        UUID recordId,
        @Schema(description = "배송 id")
        UUID deliveryId,
        @Schema(description = "수령 업체 id")
        UUID receiverCompanyId,
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
        @Schema(description = "업체 배송 경로 상태")
        CompanyRouteRecordStatus status
    ) {
        public static Stop from(CompanyDeliveryRouteRecord record) {
            return Stop.builder()
                .recordId(record.getId())
                .deliveryId(record.getDeliveryId())
                .receiverCompanyId(record.getReceiverCompanyId())
                .latitude(record.getLatitude())
                .longitude(record.getLongitude())
                .estimatedDistance(record.getEstimatedDistance())
                .estimatedDuration(record.getEstimatedDuration())
                .deliverySequence(record.getDeliverySequence())
                .status(record.getStatus())
                .build();
        }
    }

    public static TodayRouteResponseDto from(DeliveryAgent agent, List<CompanyDeliveryRouteRecord> records) {
        return TodayRouteResponseDto.builder()
            .agentId(agent.getId())
            .totalDistance(agent.getTotalDistance())
            .totalDuration(agent.getTotalDuration())
            .routeComputedAt(agent.getRouteComputedAt())
            .stops(records.stream().map(Stop::from).toList())
            .build();
    }
}
