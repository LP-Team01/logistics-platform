package com.logistics.order.outbox;

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
public class DeliveryCompensationOutboxPublisher {

    private final DeliveryCompensationOutboxRepository repository;
    private final KafkaTemplate<String, DeliveryCompensationEvent> kafkaTemplate;

    @Value("${kafka.topic.delivery-compensation}")
    private String topic;

    @Scheduled(fixedDelayString = "${kafka.outbox.publish-interval-ms:5000}")
    @Transactional
    public void publishPending() {
        repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()
            .forEach(this::publish);
    }

    private void publish(DeliveryCompensationOutbox outbox) {
        try {
            DeliveryCompensationEvent event = new DeliveryCompensationEvent(
                outbox.getEventId(), outbox.getOrderId(), outbox.getCreatedAt());
            kafkaTemplate.send(topic, outbox.getOrderId().toString(), event)
                .get(10, TimeUnit.SECONDS);
            outbox.markPublished();
        } catch (Exception exception) {
            log.error("배송 보상 이벤트 발행 실패. eventId={}, orderId={}",
                outbox.getEventId(), outbox.getOrderId(), exception);
        }
    }
}
