package com.logistics.delivery.command.dto.request;

import com.logistics.delivery.command.dto.command.CreateDeliveryCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "배송 생성 요청")
public record CreateDeliveryRequestDto(
    @Schema(description = "주문 id")
    @NotNull(message = "주문 id는 필수입니다.")
    UUID orderId,

    @Schema(description = "주문아이템 id")
    @NotNull(message = "주문아이템 id는 필수입니다.")
    UUID orderItemId,

    @Schema(description = "출발 허브 id")
    @NotNull(message = "출발 허브 id는 필수입니다.")
    UUID departureHubId,

    @Schema(description = "목적지 허브 id")
    @NotNull(message = "목적지 허브 id는 필수입니다.")
    UUID destinationHubId,

    @Schema(description = "최종 배송지 주소")
    @NotNull(message = "최종 배송지 주소는 필수입니다.")
    String deliveryAddress,

    @Schema(description = "수령인 이름")
    @NotNull(message = "수령인 이름은 필수입니다.")
    String receiver,

    @Schema(description = "수령인 Slack id")
    String receiverSlackId,

    @Schema(description = "수령할 업체 id")
    @NotNull(message = "수령할 업체 id는 필수입니다.")
    UUID receiverCompanyId,

    @Schema(description = "수령인 사용자 id")
    @NotNull(message = "수령인 사용자 id는 필수입니다.")
    UUID recipientUserId,

    @Schema(description = "상품명")
    @NotNull(message = "상품명은 필수입니다.")
    String productName,

    @Schema(description = "상품 수량", example = "1")
    @NotNull(message = "상품 수량은 필수입니다.")
    Integer quantity,

    @Schema(description = "고객 배송 요청사항")
    String requestText,

    @Schema(description = "희망 도착 일시")
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
