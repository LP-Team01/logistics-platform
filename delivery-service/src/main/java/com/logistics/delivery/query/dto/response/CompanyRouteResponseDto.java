package com.logistics.delivery.query.dto.response;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "업체 배송 경로 조회 결과")
public record CompanyRouteResponseDto(
    @Schema(description = "업체 배송 경로 기록 id")
    UUID recordId,
    @Schema(description = "배송 id")
    UUID deliveryId,
    @Schema(description = "출발 허브 id(목적지 허브)")
    UUID departureHubId,
    @Schema(description = "수령 업체 id")
    UUID receiverCompanyId,
    @Schema(description = "예상 이동 거리(m)")
    Integer estimatedDistance,
    @Schema(description = "예상 소요 시간(분)")
    Integer estimatedDuration,
    @Schema(description = "실제 이동 거리(m)")
    Integer actualDistance,
    @Schema(description = "실제 소요 시간(분)")
    Integer actualDuration,
    @Schema(description = "업체 배송 경로 상태")
    CompanyRouteRecordStatus status,
    @Schema(description = "담당 업체 배송 담당자 id")
    UUID agentId,
    @Schema(description = "방문 순서")
    Integer deliverySequence
) {
    public static CompanyRouteResponseDto from(CompanyDeliveryRouteRecord routeRecord) {
        return CompanyRouteResponseDto.builder()
            .recordId(routeRecord.getId())
            .deliveryId(routeRecord.getDeliveryId())
            .departureHubId(routeRecord.getDepartureHubId())
            .receiverCompanyId(routeRecord.getReceiverCompanyId())
            .estimatedDistance(routeRecord.getEstimatedDistance())
            .estimatedDuration(routeRecord.getEstimatedDuration())
            .actualDistance(routeRecord.getActualDistance())
            .actualDuration(routeRecord.getActualDuration())
            .status(routeRecord.getStatus())
            .agentId(routeRecord.getAgentId())
            .deliverySequence(routeRecord.getDeliverySequence())
            .build();
    }
}
