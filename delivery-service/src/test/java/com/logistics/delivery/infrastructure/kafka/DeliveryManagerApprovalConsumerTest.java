package com.logistics.delivery.infrastructure.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.logistics.delivery.command.application.DeliveryManagerApprovalService;
import com.logistics.delivery.domain.entity.AgentType;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.RetryableTopic;

class DeliveryManagerApprovalConsumerTest {

    private DeliveryManagerApprovalRequestedEvent newEvent() {
        return new DeliveryManagerApprovalRequestedEvent(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            AgentType.HUB_DELIVERY, "slack-id", UUID.randomUUID(), Instant.now());
    }

    @Test
    @DisplayName("Kafka 이벤트를 받으면 배송담당자 승인 요청 처리를 호출한다")
    void delegatesApprovalHandling() {
        DeliveryManagerApprovalService service = Mockito.mock(DeliveryManagerApprovalService.class);
        DeliveryManagerApprovalConsumer consumer = new DeliveryManagerApprovalConsumer(service);
        DeliveryManagerApprovalRequestedEvent event = newEvent();

        consumer.consume(event);

        verify(service).handleApprovalRequested(event);
    }

    @Test
    @DisplayName("승인 요청 처리 실패 예외를 Kafka Retry 처리기로 전달한다")
    void propagatesExceptionWhenHandlingFails() {
        DeliveryManagerApprovalService service = Mockito.mock(DeliveryManagerApprovalService.class);
        DeliveryManagerApprovalConsumer consumer = new DeliveryManagerApprovalConsumer(service);
        DeliveryManagerApprovalRequestedEvent event = newEvent();

        doThrow(new RuntimeException("approval handling failed")).when(service).handleApprovalRequested(event);

        assertThrows(RuntimeException.class, () -> consumer.consume(event));
    }

    @Test
    @DisplayName("최종 실패(DLT) 이벤트는 승인 실패 결과 이벤트 발행 처리로 위임한다")
    void delegatesDltHandlingToFinalFailureHandler() {
        DeliveryManagerApprovalService service = Mockito.mock(DeliveryManagerApprovalService.class);
        DeliveryManagerApprovalConsumer consumer = new DeliveryManagerApprovalConsumer(service);
        DeliveryManagerApprovalRequestedEvent event = newEvent();

        consumer.handleDlt(event);

        verify(service).handleApprovalRequestFailedFinally(event);
    }

    @Test
    @DisplayName("Consumer의 재시도 횟수와 백오프 설정을 검증한다")
    void configuresRetryPolicy() throws NoSuchMethodException {
        Method consume = DeliveryManagerApprovalConsumer.class.getDeclaredMethod(
            "consume", DeliveryManagerApprovalRequestedEvent.class);

        RetryableTopic retryableTopic = consume.getAnnotation(RetryableTopic.class);

        assertNotNull(retryableTopic);
        assertEquals("5", retryableTopic.attempts());
        assertEquals(5_000L, retryableTopic.backoff().delay());
        assertEquals(2.0, retryableTopic.backoff().multiplier());
    }

    @Test
    @DisplayName("최종 실패 이벤트를 처리하는 DLT Handler가 선언됐는지 검증한다")
    void configuresDltHandler() throws NoSuchMethodException {
        Method handleDlt = DeliveryManagerApprovalConsumer.class.getDeclaredMethod(
            "handleDlt", DeliveryManagerApprovalRequestedEvent.class);

        assertNotNull(handleDlt.getAnnotation(DltHandler.class));
    }
}