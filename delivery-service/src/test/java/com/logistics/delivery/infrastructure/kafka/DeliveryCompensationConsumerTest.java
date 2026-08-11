package com.logistics.delivery.infrastructure.kafka;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.logistics.delivery.command.application.DeliveryCommandService;
import java.time.Instant;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.RetryableTopic;

class DeliveryCompensationConsumerTest {

    /** Kafka 이벤트를 받으면 주문 ID 기준 배송 취소를 호출합니다. */
    @Test
    void delegatesCancellationByOrderId() {
        DeliveryCommandService service = Mockito.mock(DeliveryCommandService.class);
        DeliveryCompensationConsumer consumer = new DeliveryCompensationConsumer(service);
        UUID orderId = UUID.randomUUID();

        consumer.consume(new DeliveryCompensationEvent(UUID.randomUUID(), orderId, Instant.now()));

        verify(service).cancelByOrderId(orderId);
    }

    /** 배송 취소 실패 예외를 Kafka Retry 처리기로 전달합니다. */
    @Test
    void propagatesExceptionWhenCancellationFails() {
        DeliveryCommandService service = Mockito.mock(DeliveryCommandService.class);
        DeliveryCompensationConsumer consumer = new DeliveryCompensationConsumer(service);
        UUID orderId = UUID.randomUUID();
        DeliveryCompensationEvent event = new DeliveryCompensationEvent(
                UUID.randomUUID(), orderId, Instant.now());

        doThrow(new RuntimeException("Delivery cancellation failed"))
                .when(service).cancelByOrderId(orderId);

        assertThrows(RuntimeException.class, () -> consumer.consume(event));
    }

    /** Consumer의 재시도 횟수와 백오프 설정을 검증합니다. */
    @Test
    void configuresRetryPolicy() throws NoSuchMethodException {
        Method consume = DeliveryCompensationConsumer.class.getDeclaredMethod(
                "consume", DeliveryCompensationEvent.class);

        RetryableTopic retryableTopic = consume.getAnnotation(RetryableTopic.class);

        assertNotNull(retryableTopic);
        assertEquals("5", retryableTopic.attempts());
        assertEquals(5_000L, retryableTopic.backoff().delay());
        assertEquals(2.0, retryableTopic.backoff().multiplier());
    }

    /** 최종 실패 이벤트를 처리하는 DLT Handler가 선언됐는지 검증합니다. */
    @Test
    void configuresDltHandler() throws NoSuchMethodException {
        Method handleDlt = DeliveryCompensationConsumer.class.getDeclaredMethod(
                "handleDlt", DeliveryCompensationEvent.class);

        assertNotNull(handleDlt.getAnnotation(DltHandler.class));
    }
}
