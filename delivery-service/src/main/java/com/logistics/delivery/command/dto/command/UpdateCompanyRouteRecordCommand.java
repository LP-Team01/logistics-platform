package com.logistics.delivery.command.dto.command;

import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import lombok.Builder;

@Builder
public record UpdateCompanyRouteRecordCommand(
    CompanyRouteRecordStatus status,
    Integer actualDistance,
    Integer actualDuration
) {
}
