package com.logistics.ai.kafka.consumer;

import com.logistics.ai.global.exception.BusinessException;
import com.logistics.ai.global.exception.ErrorCode;
import com.logistics.ai.kafka.event.DeliveryAiNotificationEvent;
import com.logistics.ai.kafka.service.AiNotificationEventService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 배송 서비스가 발행한 AI 알림 이벤트를 수신합니다.
 *
 * <p>Kafka 메시지 수신과 검증만 담당하며,
 * 실제 AI 계산 및 Slack 발송은
 * AiNotificationEventService에 위임합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryAiNotificationConsumer {

    private final AiNotificationEventService eventService;
    private final Validator validator;

    /**
     * 배송 AI 알림 이벤트를 수신합니다.
     *
     * @param record Kafka Consumer Record
     */
    @KafkaListener(
        topics = "${kafka.topic.delivery-ai-notification}"
    )
    public void consume(
        ConsumerRecord<String, DeliveryAiNotificationEvent> record
    ) {
        DeliveryAiNotificationEvent event = record.value();

        validateEvent(event);

        log.info(
            "Kafka 배송 AI 알림 이벤트를 수신했습니다. "
                + "topic={}, partition={}, offset={}, "
                + "eventId={}, orderId={}, deliveryId={}",
            record.topic(),
            record.partition(),
            record.offset(),
            event.eventId(),
            event.orderId(),
            event.deliveryId()
        );

        eventService.process(event);

        log.info(
            "Kafka 배송 AI 알림 이벤트 처리를 완료했습니다. "
                + "topic={}, partition={}, offset={}, eventId={}",
            record.topic(),
            record.partition(),
            record.offset(),
            event.eventId()
        );
    }

    /**
     * 역직렬화된 Kafka 이벤트의 필수 값을 검증합니다.
     *
     * @param event 검증할 배송 AI 알림 이벤트
     */
    private void validateEvent(
        DeliveryAiNotificationEvent event
    ) {
        if (event == null) {
            throw new BusinessException(
                ErrorCode.INVALID_AI_REQUEST,
                "Kafka 이벤트 본문은 필수입니다."
            );
        }

        Set<ConstraintViolation<DeliveryAiNotificationEvent>>
            violations = validator.validate(event);

        if (violations.isEmpty()) {
            return;
        }

        String validationMessage = violations.stream()
            .map(violation ->
                violation.getPropertyPath()
                    + ": "
                    + violation.getMessage()
            )
            .sorted()
            .collect(Collectors.joining(", "));

        log.error(
            "유효하지 않은 Kafka 배송 AI 알림 이벤트입니다. "
                + "eventId={}, violations={}",
            event.eventId(),
            validationMessage
        );

        throw new BusinessException(
            ErrorCode.INVALID_AI_REQUEST,
            validationMessage
        );
    }
}
