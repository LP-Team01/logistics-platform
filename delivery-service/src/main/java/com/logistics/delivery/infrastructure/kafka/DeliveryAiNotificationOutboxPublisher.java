package com.logistics.delivery.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryAiNotificationOutboxPublisher {

    private final DeliveryAiNotificationOutboxRepository repository;
    private final KafkaTemplate<String, DeliveryAiNotificationEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.delivery-ai-notification}")
    private String topic;

    @Scheduled(fixedDelayString = "${kafka.outbox.publish-interval-ms:5000}")
    @Transactional
    public void publishPending() {
        repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()
            .forEach(this::publish);
    }

    private void publish(DeliveryAiNotificationOutbox outbox) {
        try {
            DeliveryAiNotificationEvent event = objectMapper.readValue(outbox.getPayload(), DeliveryAiNotificationEvent.class);
            kafkaTemplate.send(topic, outbox.getDeliveryId().toString(), event)
                .get(10, TimeUnit.SECONDS);
            outbox.markPublished();
        } catch (Exception exception) {
            log.error("DELIVERY_CREATED 이벤트 발행 실패. eventId={}, deliveryId={}",
                outbox.getEventId(), outbox.getDeliveryId(), exception);
        }
    }
}