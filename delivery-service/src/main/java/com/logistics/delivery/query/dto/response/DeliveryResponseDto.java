package com.logistics.delivery.query.dto.response;

import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
@Schema(description = "배송 목록 조회 결과(페이징)")
public record DeliveryResponseDto(
    @Schema(description = "배송 요약 목록")
    List<DeliverSummary> content,
    @Schema(description = "전체 요소 수")
    Long totalElements,
    @Schema(description = "전체 페이지 수")
    Integer totalPages,
    @Schema(description = "페이지 크기")
    Integer size,
    @Schema(description = "현재 페이지 번호(0부터 시작)")
    Integer number
) {

    @Schema(description = "배송 요약")
    public record DeliverSummary(
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
        @Schema(description = "수령인 이름")
        String receiver,
        @Schema(description = "생성 일시")
        Instant createdAt
    ) {
        public static DeliverSummary from(Delivery delivery) {
            return new DeliverSummary(
                delivery.getId(),
                delivery.getOrderId(),
                delivery.getOrderItemId(),
                delivery.getStatus(),
                delivery.getDepartureHubId(),
                delivery.getDestinationHubId(),
                delivery.getReceiver(),
                delivery.getCreatedAt()
            );
        }
    }

    public static DeliveryResponseDto from(Page<Delivery> page) {
        return DeliveryResponseDto.builder()
            .content(page.getContent().stream().map(DeliverSummary::from).toList())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .size(page.getSize())
            .number(page.getNumber())
            .build();
    }
}
