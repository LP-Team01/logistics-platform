package com.logistics.delivery.command.dto.request;

import com.logistics.delivery.command.dto.command.UpdateDeliveryCommand;
import com.logistics.delivery.domain.entity.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDeliveryRequestDto(
    @NotNull(message = "변경할 배송 상태 값은 필수입니다.")
    DeliveryStatus status
) {
    public UpdateDeliveryCommand toCommand() {
        return UpdateDeliveryCommand.builder()
            .status(status)
            .build();
    }
}
