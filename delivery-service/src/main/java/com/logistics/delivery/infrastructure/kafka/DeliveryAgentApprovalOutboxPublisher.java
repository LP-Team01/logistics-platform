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
public class DeliveryAgentApprovalOutboxPublisher {

    private final DeliveryAgentApprovalOutboxRepository repository;
    private final KafkaTemplate<String, DeliveryAgentApprovalResultEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.delivery-agent-approval-result}")
    private String topic;

    @Scheduled(fixedDelayString = "${kafka.outbox.publish-interval-ms:5000}")
    @Transactional
    public void publishPending() {
        repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()
            .forEach(this::publish);
    }

    private void publish(DeliveryAgentApprovalOutbox outbox) {
        try {
            DeliveryAgentApprovalResultEvent event =
                objectMapper.readValue(outbox.getPayload(), DeliveryAgentApprovalResultEvent.class);
            kafkaTemplate.send(topic, outbox.getAgentId().toString(), event)
                .get(10, TimeUnit.SECONDS);
            outbox.markPublished();
        } catch (Exception exception) {
            log.error("승인 결과 이벤트 발행 실패. eventId={}, agentId={}",
                outbox.getEventId(), outbox.getAgentId(), exception);
        }
    }
}