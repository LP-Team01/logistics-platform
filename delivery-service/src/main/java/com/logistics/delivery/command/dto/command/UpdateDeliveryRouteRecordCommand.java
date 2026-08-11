package com.logistics.delivery.command.dto.command;

import com.logistics.delivery.domain.entity.RouteRecordStatus;
import lombok.Builder;

@Builder
public record UpdateDeliveryRouteRecordCommand(
    RouteRecordStatus status,
    Integer actualDistance,
    Integer actualDuration
) {
}
