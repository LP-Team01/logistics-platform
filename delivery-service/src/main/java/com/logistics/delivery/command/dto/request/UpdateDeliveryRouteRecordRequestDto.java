package com.logistics.delivery.command.dto.request;

import com.logistics.delivery.command.dto.command.UpdateDeliveryRouteRecordCommand;
import com.logistics.delivery.domain.entity.RouteRecordStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDeliveryRouteRecordRequestDto(
    @NotNull(message = "구간 배송 상태 변경 값은 필수 입니다.")
    RouteRecordStatus status,

    Integer actualDistance,

    Integer actualDuration

) {
    public UpdateDeliveryRouteRecordCommand toCommand() {
        return UpdateDeliveryRouteRecordCommand.builder()
            .status(status)
            .actualDistance(actualDistance)
            .actualDuration(actualDuration)
            .build();
    }
}
