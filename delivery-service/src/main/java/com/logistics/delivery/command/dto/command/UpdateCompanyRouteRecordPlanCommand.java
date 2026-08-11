package com.logistics.delivery.command.dto.command;

import lombok.Builder;

@Builder
public record UpdateCompanyRouteRecordPlanCommand(
    Double latitude,
    Double longitude,
    Integer estimatedDistance,
    Integer estimatedDuration,
    Integer deliverySequence
) {
}