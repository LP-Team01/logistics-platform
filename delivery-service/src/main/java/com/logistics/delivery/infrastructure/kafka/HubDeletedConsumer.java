package com.logistics.delivery.infrastructure.kafka;

import com.logistics.delivery.command.application.DeliveryAgentCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubDeletedConsumer {

    private final DeliveryAgentCommandService deliveryAgentCommandService;

    @RetryableTopic(
        attempts = "5",
        backoff = @Backoff(delay = 5_000, multiplier = 2),
        listenerContainerFactory = "hubEventsKafkaListenerContainerFactory"
    )
    @KafkaListener(
        topics = "${kafka.topic.hub-events}",
        containerFactory = "hubEventsKafkaListenerContainerFactory"
    )
    @CacheEvict(value = "delivery-service-hubs", key = "#event.hubId()")
    public void consume(HubDeletedEvent event) {
        deliveryAgentCommandService.deleteAllByHub(event.hubId(), event.deletedBy());
        log.info("허브 삭제로 소속 배송담당자 소프트삭제 및 hubs 캐시 evict 완료. hubId={}, deletedAt={}",
            event.hubId(), event.deletedAt());
    }

    @DltHandler
    public void handleDlt(HubDeletedEvent event) {
        log.error("허브 삭제 이벤트 최종 실패(DLT). hubId={}, deletedAt={}", event.hubId(), event.deletedAt());
    }
}