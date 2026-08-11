package com.logistics.delivery.command.dto.request;

import com.logistics.delivery.command.dto.command.CreateDeliveryCommand;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
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
    UUID receiverCompanyId,

    @NotNull(message = "수령인 사용자 id는 필수입니다.")
    UUID recipientUserId,

    @NotNull(message = "상품명은 필수입니다.")
    String productName,

    @NotNull(message = "상품 수량은 필수입니다.")
    Integer quantity,

    String requestText,

    @NotNull(message = "희망 도착 일시는 필수입니다.")
    Instant requestedArrivalAt
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
            .recipientUserId(recipientUserId)
            .productName(productName)
            .quantity(quantity)
            .requestText(requestText)
            .requestedArrivalAt(requestedArrivalAt)
            .build();
    }
}
