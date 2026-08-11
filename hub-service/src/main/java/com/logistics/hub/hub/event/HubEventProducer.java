package com.logistics.hub.hub.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HubEventProducer {

    private final KafkaTemplate<String, HubDeletedEvent> kafkaTemplate;
    private final String topic;

    public HubEventProducer(
        KafkaTemplate<String, HubDeletedEvent> kafkaTemplate,
        @Value("${kafka.topic.hub-events}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publishHubDeleted(HubDeletedEvent event) {
        kafkaTemplate.send(topic, event.hubId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("허브 삭제 이벤트 발행 실패. hubId={}", event.hubId(), ex);
                }
            });
    }
}
