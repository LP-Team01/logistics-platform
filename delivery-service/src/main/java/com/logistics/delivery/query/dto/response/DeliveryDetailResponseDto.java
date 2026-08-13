package com.logistics.delivery.query.dto.response;

import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.DeliveryStatus;
import com.logistics.delivery.query.dto.response.DeliveryRouteResponseDto.DeliverRouteSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "배송 단건 조회 결과")
public record DeliveryDetailResponseDto(
    @Schema(description = "배송 id")
    UUID deliveryId,
    @Schema(description = "주문 id")
    UUID orderId,
    @Schema(description = "주문아이템 id")
    UUID orderItemId,
    @Schema(description = "배송 상태")
    DeliveryStatus status,
    @Schema(description = "출발 허브 id")
    UUID departureHubId,
    @Schema(description = "목적지 허브 id")
    UUID destinationHubId,
    @Schema(description = "최종 배송지 주소")
    String deliveryAddress,
    @Schema(description = "수령인 이름")
    String receiver,
    @Schema(description = "수령인 Slack id")
    String receiverSlackId,
    @Schema(description = "업체 배송 담당자 id")
    UUID companyAgentId,
    @Schema(description = "허브 간 경로 기록 목록")
    List<DeliverRouteSummary> routeRecords,
    @Schema(description = "생성 일시")
    Instant createdAt,
    @Schema(description = "수정 일시")
    Instant updatedAt
) {
    public static DeliveryDetailResponseDto from(Delivery delivery, List<DeliveryRouteRecord> routeRecords) {
        return DeliveryDetailResponseDto.builder()
            .deliveryId(delivery.getId())
            .orderId(delivery.getOrderId())
            .orderItemId(delivery.getOrderItemId())
            .status(delivery.getStatus())
            .departureHubId(delivery.getDepartureHubId())
            .destinationHubId(delivery.getDestinationHubId())
            .deliveryAddress(delivery.getDeliveryAddress())
            .receiver(delivery.getReceiver())
            .receiverSlackId(delivery.getReceiverSlackId())
            .companyAgentId(delivery.getCompanyAgentId())
            .routeRecords(routeRecords.stream().map(DeliverRouteSummary::from).toList())
            .createdAt(delivery.getCreatedAt())
            .updatedAt(delivery.getUpdatedAt())
            .build();
    }
}
