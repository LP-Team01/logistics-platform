package com.logistics.delivery.query.dto.response;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

// 업체배송담당자의 당일 방문 계획 - "매일 아침 6시 발송" 트리거 담당 서비스가 조회
@Builder
public record TodayRouteResponseDto(
    UUID agentId,
    Integer totalDistance,
    Integer totalDuration,
    Instant routeComputedAt,
    List<Stop> stops
) {
    @Builder
    public record Stop(
        UUID recordId,
        UUID deliveryId,
        UUID receiverCompanyId,
        Double latitude,
        Double longitude,
        Integer estimatedDistance,
        Integer estimatedDuration,
        Integer deliverySequence,
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
