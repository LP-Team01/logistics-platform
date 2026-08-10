package com.logistics.delivery.infrastructure.kafka;

import com.logistics.delivery.command.application.DeliveryCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryCompensationConsumer {

    private final DeliveryCommandService deliveryCommandService;

    @RetryableTopic(
        attempts = "5",
        backoff = @Backoff(delay = 5_000, multiplier = 2)
    )
    @KafkaListener(topics = "${kafka.topic.delivery-compensation}")
    public void consume(DeliveryCompensationEvent event) {
        deliveryCommandService.cancelByOrderId(event.orderId());
        log.info("배송 보상 처리 완료. eventId={}, orderId={}", event.eventId(), event.orderId());
    }

    @DltHandler
    public void handleDlt(DeliveryCompensationEvent event) {
        log.error("배송 보상 최종 실패(DLT). eventId={}, orderId={}",
            event.eventId(), event.orderId());
    }
}
