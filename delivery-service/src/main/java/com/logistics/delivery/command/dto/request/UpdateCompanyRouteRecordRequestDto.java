package com.logistics.delivery.command.dto.request;

import com.logistics.delivery.command.dto.command.UpdateCompanyRouteRecordCommand;
import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCompanyRouteRecordRequestDto(
    @NotNull(message = "업체 배송 상태 변경 값은 필수 입니다.")
    CompanyRouteRecordStatus status,

    Integer actualDistance,

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
