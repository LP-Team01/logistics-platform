package com.logistics.delivery.infrastructure.kafka;

import com.logistics.delivery.command.application.DeliveryManagerApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryManagerApprovalConsumer {

    private final DeliveryManagerApprovalService deliveryManagerApprovalService;

    @RetryableTopic(
        attempts = "5",
        backoff = @Backoff(delay = 5_000, multiplier = 2),
        listenerContainerFactory = "deliveryManagerApprovalKafkaListenerContainerFactory"
    )
    @KafkaListener(
        topics = "${kafka.topic.delivery-manager-approval-requested}",
        containerFactory = "deliveryManagerApprovalKafkaListenerContainerFactory"
    )
    public void consume(DeliveryManagerApprovalRequestedEvent event) {
        deliveryManagerApprovalService.handleApprovalRequested(event);
        log.info("배송담당자 승인 요청 처리 완료. agentId={}", event.agentId());
    }

    @DltHandler
    public void handleDlt(DeliveryManagerApprovalRequestedEvent event) {
        log.error("배송담당자 승인 요청 최종 실패(DLT). agentId={}", event.agentId());
        deliveryManagerApprovalService.handleApprovalRequestFailedFinally(event);
    }
}