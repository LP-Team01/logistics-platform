package com.logistics.ai.kafka.mapper;

import com.logistics.ai.airequest.dto.requestdto.AiRequestDto;
import com.logistics.ai.airequest.dto.responsedto.AiResponseDto;
import com.logistics.ai.airequest.entity.AiRequestStatus;
import com.logistics.ai.kafka.event.DeliveryAiNotificationEvent;
import com.logistics.ai.kafka.event.DeliveryAiNotificationEventType;
import com.logistics.ai.slackmessage.dto.requestdto.SlackMessageRequestDto;
import com.logistics.ai.slackmessage.entity.SlackMessageType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiNotificationEventMapperTest {

    private AiNotificationEventMapper eventMapper;

    @BeforeEach
    void setUp() {
        eventMapper = new AiNotificationEventMapper();
    }

    @Test
    @DisplayName("Kafka 배송 이벤트를 AI 요청 DTO로 변환한다")
    void toAiRequestDto() {
        // given
        DeliveryAiNotificationEvent event = createEvent();

        // when
        AiRequestDto result =
            eventMapper.toAiRequestDto(event);

        // then
        assertThat(result.eventId())
            .isEqualTo(event.eventId());

        assertThat(result.orderId())
            .isEqualTo(event.orderId());

        assertThat(result.deliveryId())
            .isEqualTo(event.deliveryId());

        assertThat(result.requestText())
            .isEqualTo(event.requestText());

        assertThat(result.departureLocation())
            .isEqualTo(event.departureLocation());

        assertThat(result.waypointLocations())
            .containsExactlyElementsOf(
                event.waypointLocations()
            );

        assertThat(result.destinationLocation())
            .isEqualTo(event.destinationLocation());

        assertThat(result.requestedArrivalAt())
            .isEqualTo(event.requestedArrivalAt());

        assertThat(result.estimatedDurationMinutes())
            .isEqualTo(event.estimatedDurationMinutes());

        assertThat(result.preparationBufferMinutes())
            .isEqualTo(event.preparationBufferMinutes());

        assertThat(result.products())
            .hasSize(2);

        assertThat(result.products().get(0).productName())
            .isEqualTo("냉동 닭가슴살");

        assertThat(result.products().get(0).quantity())
            .isEqualTo(10);
    }

    @Test
    @DisplayName("AI 계산 결과를 Slack 메시지 요청으로 변환한다")
    void toSlackMessageRequestDto() {
        // given
        DeliveryAiNotificationEvent event = createEvent();

        UUID aiRequestId = UUID.randomUUID();

        LocalDateTime dispatchDeadline =
            LocalDateTime.of(2026, 8, 6, 14, 0);

        AiResponseDto aiResponse =
            createAiResponse(
                event,
                aiRequestId,
                dispatchDeadline
            );

        // when
        SlackMessageRequestDto result =
            eventMapper.toSlackMessageRequestDto(
                event,
                aiResponse
            );

        // then
        assertThat(result.aiRequestId())
            .isEqualTo(aiRequestId);

        assertThat(result.recipientUserId())
            .isEqualTo(event.recipientUserId());

        assertThat(result.recipientSlackId())
            .isEqualTo(event.recipientSlackId());

        assertThat(result.messageType())
            .isEqualTo(
                SlackMessageType.DISPATCH_DEADLINE
            );

        assertThat(result.title())
            .isEqualTo("최종 발송 시한 안내");

        assertThat(result.content())
            .contains(event.orderId().toString())
            .contains(event.deliveryId().toString())
            .contains("2026-08-06 14:00")
            .contains("2026-08-06 18:00");
    }

    /**
     * 테스트용 Kafka 배송 이벤트를 생성합니다.
     */
    private DeliveryAiNotificationEvent createEvent() {
        return new DeliveryAiNotificationEvent(
            UUID.randomUUID(),
            "v1",
            DeliveryAiNotificationEventType.DELIVERY_CREATED,
            Instant.parse("2026-08-05T12:30:00Z"),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "U0123456789",
            List.of(
                new DeliveryAiNotificationEvent.ProductItem(
                    "냉동 닭가슴살",
                    10
                ),
                new DeliveryAiNotificationEvent.ProductItem(
                    "생수 2L",
                    5
                )
            ),
            "오후 6시 이전에 배송해 주세요.",
            "서울특별시 구로구",
            List.of(
                "경기 남부 허브",
                "서울 서부 허브"
            ),
            "서울특별시 관악구",
            LocalDateTime.of(2026, 8, 6, 18, 0),
            180,
            60
        );
    }

    /**
     * 테스트용 AI 성공 응답을 생성합니다.
     */
    private AiResponseDto createAiResponse(
        DeliveryAiNotificationEvent event,
        UUID aiRequestId,
        LocalDateTime dispatchDeadline
    ) {
        return new AiResponseDto(
            aiRequestId,
            event.eventId(),
            event.orderId(),
            event.deliveryId(),
            event.requestText(),
            event.requestedArrivalAt(),
            event.estimatedDurationMinutes(),
            event.preparationBufferMinutes(),
            "최종 발송 시한을 계산해주세요.",
            """
            {
              "dispatchDeadline": "2026-08-06T14:00:00"
            }
            """,
            dispatchDeadline,
            AiRequestStatus.SUCCESS,
            "gemini-3.6-flash",
            "v1",
            1500L,
            null,
            UUID.randomUUID(),
            Instant.parse("2026-08-05T03:30:00Z"),
            null,
            null
        );
    }
}
