package com.logistics.delivery.command.dto.request;

import com.logistics.delivery.command.dto.command.UpdateCompanyRouteRecordCommand;
import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "업체 배송 경로 상태 변경 요청")
public record UpdateCompanyRouteRecordRequestDto(
    @Schema(description = "변경할 업체 배송 경로 상태")
    @NotNull(message = "업체 배송 상태 변경 값은 필수 입니다.")
    CompanyRouteRecordStatus status,

    @Schema(description = "실제 이동 거리(m)")
    Integer actualDistance,

    @Schema(description = "실제 소요 시간(분)")
    Integer actualDuration

) {
    public UpdateCompanyRouteRecordCommand toCommand() {
        return UpdateCompanyRouteRecordCommand.builder()
            .status(status)
            .actualDistance(actualDistance)
            .actualDuration(actualDuration)
            .build();
    }
}
