package com.logistics.ai.airequest.dto.requestdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 최종 발송 시한을 계산하기 위한 AI 요청 DTO입니다.
 */
@Schema(description = "AI 최종 발송 시한 생성 요청")
public record AiRequestDto(

    @NotNull(message = "이벤트 ID는 필수입니다.")
    @Schema(description = "Kafka 이벤트 식별자")
    UUID eventId,

    @NotNull(message = "주문 ID는 필수입니다.")
    @Schema(description = "주문 식별자")
    UUID orderId,

    @NotNull(message = "배송 ID는 필수입니다.")
    @Schema(description = "배송 식별자")
    UUID deliveryId,

    /**
     * 주문에 포함된 상품과 수량입니다.
     */
    @NotEmpty(message = "상품 정보는 한 개 이상 필요합니다.")
    @Valid
    @Schema(description = "상품 및 수량 정보")
    List<ProductItemDto> products,

    /**
     * 고객이 입력한 배송 요청사항입니다.
     */
    @Size(
        max = 2000,
        message = "배송 요청사항은 2000자 이하여야 합니다."
    )
    @Schema(description = "고객 배송 요청사항")
    String requestText,

    /**
     * 배송이 출발하는 업체 또는 허브 정보입니다.
     */
    @NotBlank(message = "발송지 정보는 필수입니다.")
    @Size(max = 500, message = "발송지 정보는 500자 이하여야 합니다.")
    @Schema(description = "발송지 정보")
    String departureLocation,

    /**
     * 배송 과정에서 거치는 허브 목록입니다.
     *
     * <p>경유지가 없는 경우 빈 배열을 전달할 수 있습니다.</p>
     */
    @NotNull(message = "경유지 목록은 필수입니다.")
    @Schema(description = "경유지 목록")
    List<
        @NotBlank(message = "경유지 정보는 비어 있을 수 없습니다.")
            String
        > waypointLocations,

    /**
     * 최종 배송 목적지입니다.
     */
    @NotBlank(message = "도착지 정보는 필수입니다.")
    @Size(max = 500, message = "도착지 정보는 500자 이하여야 합니다.")
    @Schema(description = "도착지 정보")
    String destinationLocation,

    /**
     * 고객이 요청한 최종 도착 일시입니다.
     */
    @NotNull(message = "희망 도착 일시는 필수입니다.")
    @Future(message = "희망 도착 일시는 현재 시각 이후여야 합니다.")
    @Schema(description = "희망 도착 일시")
    LocalDateTime requestedArrivalAt,

    /**
     * 전체 경로의 예상 배송시간입니다.
     */
    @NotNull(message = "예상 배송시간은 필수입니다.")
    @Positive(message = "예상 배송시간은 0보다 커야 합니다.")
    @Schema(description = "예상 배송시간(분)")
    Integer estimatedDurationMinutes,

    /**
     * 포장과 상품 준비에 필요한 여유 시간입니다.
     */
    @PositiveOrZero(message = "배송 준비시간은 0 이상이어야 합니다.")
    @Schema(description = "배송 준비시간(분)", defaultValue = "0")
    Integer preparationBufferMinutes
) {

    /**
     * AI에 전달할 상품명과 주문 수량입니다.
     */
    @Schema(description = "주문 상품 정보")
    public record ProductItemDto(

        @NotBlank(message = "상품명은 필수입니다.")
        @Schema(description = "상품명")
        String productName,

        @NotNull(message = "상품 수량은 필수입니다.")
        @Positive(message = "상품 수량은 0보다 커야 합니다.")
        @Schema(description = "주문 수량")
        Integer quantity
    ) {
    }
}
