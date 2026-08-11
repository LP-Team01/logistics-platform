package com.logistics.delivery.command.dto.request;

import com.logistics.delivery.command.dto.command.UpdateDeliveryRouteRecordCommand;
import com.logistics.delivery.domain.entity.RouteRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "허브 경로 기록 상태 변경 요청")
public record UpdateDeliveryRouteRecordRequestDto(
    @Schema(description = "변경할 구간 배송 상태")
    @NotNull(message = "구간 배송 상태 변경 값은 필수 입니다.")
    RouteRecordStatus status,

    @Schema(description = "실제 이동 거리(m)")
    Integer actualDistance,

    @Schema(description = "실제 소요 시간(분)")
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
