package com.logistics.ai.kafka.mapper;

import com.logistics.ai.airequest.dto.requestdto.AiRequestDto;
import com.logistics.ai.airequest.dto.responsedto.AiResponseDto;
import com.logistics.ai.kafka.event.DeliveryAiNotificationEvent;
import com.logistics.ai.slackmessage.dto.requestdto.SlackMessageRequestDto;
import com.logistics.ai.slackmessage.entity.SlackMessageType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Kafka 배송 이벤트를 AI 요청 및 Slack 메시지 요청으로 변환합니다.
 *
 * <p>Kafka 이벤트 계약이 내부 서비스 DTO에 직접 의존하지 않도록
 * 변환 책임을 별도의 Mapper로 분리합니다.</p>
 */
@Component
public class AiNotificationEventMapper {

    private static final String SLACK_MESSAGE_TITLE =
        "최종 발송 시한 안내";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Kafka 배송 이벤트를 AI 요청 DTO로 변환합니다.
     *
     * @param event 배송 AI 알림 이벤트
     * @return AI 최종 발송 시한 계산 요청
     */
    public AiRequestDto toAiRequestDto(
        DeliveryAiNotificationEvent event
    ) {
        List<AiRequestDto.ProductItemDto> products =
            event.products()
                .stream()
                .map(this::toProductItemDto)
                .toList();

        return new AiRequestDto(
            event.eventId(),
            event.orderId(),
            event.deliveryId(),
            products,
            event.requestText(),
            event.departureLocation(),
            List.copyOf(event.waypointLocations()),
            event.destinationLocation(),
            event.requestedArrivalAt(),
            event.estimatedDurationMinutes(),
            event.preparationBufferMinutes()
        );
    }

    /**
     * AI 처리 결과를 Slack 메시지 발송 요청으로 변환합니다.
     *
     * <p>이 메서드는 AI 계산이 성공하여 aiRequestId와
     * dispatchDeadline이 존재하는 경우 호출합니다.</p>
     *
     * @param event Kafka 배송 이벤트
     * @param aiResponse AI 처리 결과
     * @return Slack 메시지 발송 요청
     */
    public SlackMessageRequestDto toSlackMessageRequestDto(
        DeliveryAiNotificationEvent event,
        AiResponseDto aiResponse
    ) {
        validateAiResponse(aiResponse);

        return new SlackMessageRequestDto(
            aiResponse.aiRequestId(),
            event.recipientUserId(),
            event.recipientSlackId(),
            SlackMessageType.DISPATCH_DEADLINE,
            SLACK_MESSAGE_TITLE,
            createSlackMessageContent(event, aiResponse)
        );
    }

    /**
     * Kafka 이벤트 상품을 AI 요청 상품 DTO로 변환합니다.
     */
    private AiRequestDto.ProductItemDto toProductItemDto(
        DeliveryAiNotificationEvent.ProductItem product
    ) {
        return new AiRequestDto.ProductItemDto(
            product.productName(),
            product.quantity()
        );
    }

    /**
     * AI 계산 결과를 이용하여 Slack 메시지 본문을 생성합니다.
     */
    private String createSlackMessageContent(
        DeliveryAiNotificationEvent event,
        AiResponseDto aiResponse
    ) {
        return """
            주문의 최종 발송 시한이 계산되었습니다.

            주문 ID: %s
            배송 ID: %s
            최종 발송 시한: %s
            희망 도착 시각: %s
            """
            .formatted(
                event.orderId(),
                event.deliveryId(),
                formatDateTime(aiResponse.dispatchDeadline()),
                formatDateTime(event.requestedArrivalAt())
            )
            .stripIndent()
            .trim();
    }

    /**
     * Slack 메시지를 만들기 전에 AI 결과의 필수 값을 검증합니다.
     */
    private void validateAiResponse(AiResponseDto aiResponse) {
        if (aiResponse == null) {
            throw new IllegalArgumentException(
                "AI 처리 결과는 필수입니다."
            );
        }

        if (aiResponse.aiRequestId() == null) {
            throw new IllegalArgumentException(
                "AI 요청 ID는 필수입니다."
            );
        }

        if (aiResponse.dispatchDeadline() == null) {
            throw new IllegalArgumentException(
                "AI 최종 발송 시한은 필수입니다."
            );
        }
    }

    /**
     * Slack 메시지에 표시할 날짜 형식으로 변환합니다.
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }
}
