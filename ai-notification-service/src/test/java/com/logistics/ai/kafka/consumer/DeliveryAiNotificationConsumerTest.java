package com.logistics.ai.kafka.consumer;

import com.logistics.ai.global.exception.BusinessException;
import com.logistics.ai.global.exception.ErrorCode;
import com.logistics.ai.kafka.event.DeliveryAiNotificationEvent;
import com.logistics.ai.kafka.event.DeliveryAiNotificationEventType;
import com.logistics.ai.kafka.service.AiNotificationEventService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DeliveryAiNotificationConsumerTest {

    @Mock
    private AiNotificationEventService eventService;

    private ValidatorFactory validatorFactory;
    private DeliveryAiNotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        validatorFactory =
            Validation.buildDefaultValidatorFactory();

        Validator validator =
            validatorFactory.getValidator();

        consumer =
            new DeliveryAiNotificationConsumer(
                eventService,
                validator
            );
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("정상 Kafka 이벤트를 이벤트 처리 서비스에 전달한다")
    void consumeValidEvent() {
        // given
        DeliveryAiNotificationEvent event =
            createValidEvent();

        ConsumerRecord<String, DeliveryAiNotificationEvent>
            record =
            new ConsumerRecord<>(
                "delivery-ai-notification",
                0,
                1L,
                event.eventId().toString(),
                event
            );

        // when
        consumer.consume(record);

        // then
        verify(eventService).process(event);
    }

    @Test
    @DisplayName("Kafka 이벤트 본문이 null이면 예외가 발생한다")
    void consumeNullEvent() {
        // given
        ConsumerRecord<String, DeliveryAiNotificationEvent>
            record =
            new ConsumerRecord<>(
                "delivery-ai-notification",
                0,
                1L,
                "event-key",
                null
            );

        // when & then
        assertThatExceptionOfType(BusinessException.class)
            .isThrownBy(() -> consumer.consume(record))
            .satisfies(exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_AI_REQUEST)
            );

        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("필수 값이 누락된 Kafka 이벤트는 처리하지 않는다")
    void consumeInvalidEvent() {
        // given
        DeliveryAiNotificationEvent invalidEvent =
            new DeliveryAiNotificationEvent(
                null,
                "v1",
                DeliveryAiNotificationEventType.DELIVERY_CREATED,
                Instant.parse("2026-08-05T12:30:00Z"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "",
                List.of(),
                "배송 요청사항",
                "서울특별시 구로구",
                List.of(),
                "서울특별시 관악구",
                LocalDateTime.of(2026, 8, 6, 18, 0),
                180,
                60
            );

        ConsumerRecord<String, DeliveryAiNotificationEvent>
            record =
            new ConsumerRecord<>(
                "delivery-ai-notification",
                0,
                1L,
                "invalid-event",
                invalidEvent
            );

        // when & then
        assertThatExceptionOfType(BusinessException.class)
            .isThrownBy(() -> consumer.consume(record))
            .satisfies(exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_AI_REQUEST)
            );

        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("지원하지 않는 Kafka 이벤트 버전은 처리하지 않는다")
    void consumeUnsupportedEventVersion() {
        // given
        DeliveryAiNotificationEvent event =
            createEventWithVersion("v999");

        ConsumerRecord<String, DeliveryAiNotificationEvent> record =
            new ConsumerRecord<>(
                "delivery-ai-notification",
                0,
                1L,
                event.eventId().toString(),
                event
            );

        // when & then
        assertThatExceptionOfType(BusinessException.class)
            .isThrownBy(() -> consumer.consume(record))
            .satisfies(exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(
                        ErrorCode.UNSUPPORTED_EVENT_VERSION
                    )
            );

        verifyNoInteractions(eventService);
    }

    private DeliveryAiNotificationEvent createValidEvent() {
        return createEventWithVersion("v1");
    }

    private DeliveryAiNotificationEvent createEventWithVersion(
        String eventVersion
    ) {
        return new DeliveryAiNotificationEvent(
            UUID.randomUUID(),
            eventVersion,
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
                )
            ),
            "오후 6시 이전에 배송해 주세요.",
            "서울특별시 구로구",
            List.of("경기 남부 허브"),
            "서울특별시 관악구",
            LocalDateTime.of(2026, 8, 6, 18, 0),
            180,
            60
        );
    }
}
