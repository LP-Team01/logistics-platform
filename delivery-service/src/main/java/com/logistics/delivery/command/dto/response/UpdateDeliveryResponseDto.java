package com.logistics.delivery.command.dto.response;

import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.DeliveryStatus;
import com.logistics.delivery.domain.entity.RouteRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "배송 상태 변경 결과")
public record UpdateDeliveryResponseDto(
        @Schema(description = "배송 id")
        UUID deliveryId,
        @Schema(description = "변경된 배송 상태")
        DeliveryStatus status,
        @Schema(description = "수정 일시")
        Instant updatedAt
) {
    public static UpdateDeliveryResponseDto from(Delivery delivery) {
        return UpdateDeliveryResponseDto.builder()
                .deliveryId(delivery.getId())
                .status(delivery.getStatus())
                .updatedAt(delivery.getUpdatedAt())
                .build();
    }
}
