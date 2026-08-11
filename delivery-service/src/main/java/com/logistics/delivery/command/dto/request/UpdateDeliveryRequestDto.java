package com.logistics.delivery.command.dto.request;

import com.logistics.delivery.command.dto.command.UpdateDeliveryCommand;
import com.logistics.delivery.domain.entity.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "배송 상태 변경 요청")
public record UpdateDeliveryRequestDto(
    @Schema(description = "변경할 배송 상태")
    @NotNull(message = "변경할 배송 상태 값은 필수입니다.")
    DeliveryStatus status
) {
    public UpdateDeliveryCommand toCommand() {
        return UpdateDeliveryCommand.builder()
            .status(status)
            .build();
    }
}
