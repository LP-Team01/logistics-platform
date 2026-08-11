package com.logistics.delivery.command.dto.request;

import com.logistics.delivery.command.dto.command.CreateDeliveryCommand;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDeliveryRequestDto(
    @NotNull(message = "주문 id는 필수입니다.")
    UUID orderId,

    @NotNull(message = "주문아이템 id는 필수입니다.")
    UUID orderItemId,

    @NotNull(message = "출발 허브 id는 필수입니다.")
    UUID departureHubId,

    @NotNull(message = "목적지 허브 id는 필수입니다.")
    UUID destinationHubId,

    @NotNull(message = "최종 배송지 주소는 필수입니다.")
    String deliveryAddress,

    @NotNull(message = "수령인 이름은 필수입니다.")
    String receiver,

    String receiverSlackId,

    @NotNull(message = "수령할 업체 id는 필수입니다.")
    UUID receiverCompanyId
) {
    public CreateDeliveryCommand toCommand() {
        return CreateDeliveryCommand.builder()
            .orderId(orderId)
            .orderItemId(orderItemId)
            .departureHubId(departureHubId)
            .destinationHubId(destinationHubId)
            .deliveryAddress(deliveryAddress)
            .receiver(receiver)
            .receiverSlackId(receiverSlackId)
            .receiverCompanyId(receiverCompanyId)
            .build();
    }
}
