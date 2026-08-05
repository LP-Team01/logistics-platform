package com.logistics.delivery.query.dto.reponse;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.RouteRecordStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CompanyRouteResponseDto(
    UUID recordId,
    UUID deliveryId,
    UUID departureHubId,
    UUID receiverCompanyId,
    Integer estimatedDistance,
    Integer estimatedDuration,
    Integer actualDistance,
    Integer actualDuration,
    CompanyRouteRecordStatus status,
    UUID agentId,
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
            .build();
    }
}
