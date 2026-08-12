package com.logistics.user.kafka.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.user.kafka.DeliveryManagerApprovalRequestedEvent;
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
public class DeliveryManagerApprovalOutboxPublisher {

    // 이 횟수를 넘겨도 재시도는 계속하되, 사람이 알아챌 수 있게 로그 심각도를 올린다.
    private static final int ALERT_THRESHOLD = 20;

    private final DeliveryManagerApprovalOutboxRepository repository;
    private final KafkaTemplate<String, DeliveryManagerApprovalRequestedEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.delivery-manager-approval-requested}")
    private String topic;

    @Scheduled(fixedDelayString = "${kafka.outbox.publish-interval-ms:5000}")
    @Transactional
    public void publishPending() {
        repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()
            .forEach(this::publish);
    }

    private void publish(DeliveryManagerApprovalOutbox outbox) {
        try {
            DeliveryManagerApprovalRequestedEvent event =
                objectMapper.readValue(outbox.getPayload(), DeliveryManagerApprovalRequestedEvent.class);
            kafkaTemplate.send(topic, outbox.getAgentId().toString(), event)
                .get(10, TimeUnit.SECONDS);
            outbox.markPublished();
        } catch (Exception exception) {
            outbox.incrementAttempt();
            if (outbox.getAttemptCount() >= ALERT_THRESHOLD) {
                log.error("승인 요청 이벤트 발행이 {}회 연속 실패했습니다. 수동 확인이 필요합니다. eventId={}, agentId={}",
                    outbox.getAttemptCount(), outbox.getEventId(), outbox.getAgentId(), exception);
            } else {
                log.error("승인 요청 이벤트 발행 실패. eventId={}, agentId={}, attemptCount={}",
                    outbox.getEventId(), outbox.getAgentId(), outbox.getAttemptCount(), exception);
            }
        }
    }
}
