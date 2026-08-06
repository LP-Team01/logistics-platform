package com.logistics.ai.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 배송 서비스에서 AI 알림 서비스로 전달하는 Kafka 이벤트입니다.
 *
 * <p>AI 최종 발송 시한 계산과 Slack 알림 발송에
 * 필요한 정보를 포함합니다.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryAiNotificationEvent(

    /**
     * 이벤트의 고유 식별자입니다.
     *
     * <p>동일한 Kafka 이벤트가 중복 전달되더라도
     * AI와 Slack 처리가 한 번만 수행되도록 사용합니다.</p>
     */
    @NotNull(message = "이벤트 ID는 필수입니다.")
    UUID eventId,

    /**
     * 이벤트 계약 버전입니다.
     *
     * <p>최초 버전은 v1을 사용합니다.</p>
     */
    @NotBlank(message = "이벤트 버전은 필수입니다.")
    @Size(
        max = 20,
        message = "이벤트 버전은 20자를 초과할 수 없습니다."
    )
    String eventVersion,

    /**
     * AI 알림 서비스가 처리할 이벤트 유형입니다.
     */
    @NotNull(message = "이벤트 유형은 필수입니다.")
    DeliveryAiNotificationEventType eventType,

    /**
     * 이벤트가 최초 발생한 시각입니다.
     *
     * <p>서비스 간 시간대 차이를 줄이기 위해 UTC 기반
     * Instant 타입을 사용합니다.</p>
     */
    @NotNull(message = "이벤트 발생 시각은 필수입니다.")
    Instant occurredAt,

    /**
     * 주문 식별자입니다.
     */
    @NotNull(message = "주문 ID는 필수입니다.")
    UUID orderId,

    /**
     * 배송 식별자입니다.
     */
    @NotNull(message = "배송 ID는 필수입니다.")
    UUID deliveryId,

    /**
     * Slack 알림을 받을 물류 시스템 사용자 식별자입니다.
     */
    @NotNull(message = "수신 사용자 ID는 필수입니다.")
    UUID recipientUserId,

    /**
     * Slack에서 사용하는 수신자 ID입니다.
     */
    @NotBlank(message = "Slack 수신자 ID는 필수입니다.")
    @Size(
        max = 50,
        message = "Slack 수신자 ID는 50자를 초과할 수 없습니다."
    )
    String recipientSlackId,

    /**
     * 주문에 포함된 상품 목록입니다.
     */
    @NotEmpty(message = "상품 정보는 한 개 이상 필요합니다.")
    @Size(
        max = 50,
        message = "상품은 최대 50개까지 전달할 수 있습니다."
    )
    @Valid
    List<ProductItem> products,

    /**
     * 고객이 입력한 배송 요청사항입니다.
     */
    @Size(
        max = 2000,
        message = "배송 요청사항은 2000자를 초과할 수 없습니다."
    )
    String requestText,

    /**
     * 배송 출발지 정보입니다.
     */
    @NotBlank(message = "발송지 정보는 필수입니다.")
    @Size(
        max = 500,
        message = "발송지 정보는 500자를 초과할 수 없습니다."
    )
    String departureLocation,

    /**
     * 배송 과정에서 거치는 경유지 목록입니다.
     *
     * <p>경유지가 없으면 빈 배열을 전달합니다.</p>
     */
    @NotNull(message = "경유지 목록은 필수입니다.")
    @Size(
        max = 20,
        message = "경유지는 최대 20개까지 전달할 수 있습니다."
    )
    List<
        @NotBlank(message = "경유지 정보는 비어 있을 수 없습니다.")
        @Size(
            max = 500,
            message = "경유지 정보는 500자를 초과할 수 없습니다."
        )
            String
        > waypointLocations,

    /**
     * 최종 배송 목적지 정보입니다.
     */
    @NotBlank(message = "도착지 정보는 필수입니다.")
    @Size(
        max = 500,
        message = "도착지 정보는 500자를 초과할 수 없습니다."
    )
    String destinationLocation,

    /**
     * 고객이 요청한 최종 도착 일시입니다.
     */
    @NotNull(message = "희망 도착 일시는 필수입니다.")
    LocalDateTime requestedArrivalAt,

    /**
     * 전체 경로의 예상 배송시간입니다.
     */
    @NotNull(message = "예상 배송시간은 필수입니다.")
    @Positive(message = "예상 배송시간은 0보다 커야 합니다.")
    Integer estimatedDurationMinutes,

    /**
     * 포장과 상품 준비에 필요한 여유 시간입니다.
     */
    @NotNull(message = "배송 준비시간은 필수입니다.")
    @PositiveOrZero(message = "배송 준비시간은 0 이상이어야 합니다.")
    Integer preparationBufferMinutes

) {

    /**
     * Kafka 이벤트에 포함되는 주문 상품 정보입니다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductItem(

        /**
         * 주문 상품명입니다.
         */
        @NotBlank(message = "상품명은 필수입니다.")
        @Size(
            max = 100,
            message = "상품명은 100자를 초과할 수 없습니다."
        )
        String productName,

        /**
         * 주문 상품 수량입니다.
         */
        @NotNull(message = "상품 수량은 필수입니다.")
        @Positive(message = "상품 수량은 0보다 커야 합니다.")
        Integer quantity

    ) {
    }
}
