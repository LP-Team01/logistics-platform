package com.logistics.delivery.command.dto.response;

import com.logistics.delivery.domain.entity.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "배송 일괄 생성 결과 항목")
public record BatchCreateDeliveryResponseDto(
        @Schema(description = "배송 id")
        UUID deliveryId,
        @Schema(description = "주문아이템 id")
        UUID orderItemId,
        @Schema(description = "배송 상태")
        DeliveryStatus status
) {
    public static BatchCreateDeliveryResponseDto from(CreateDeliveryResponseDto response) {
        return new BatchCreateDeliveryResponseDto(
                response.deliveryId(),
                response.orderItemId(),
                response.status()
        );
    }
}