package com.logistics.delivery.infrastructure.kafka;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.logistics.delivery.command.application.DeliveryAgentCommandService;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.RetryableTopic;

class HubDeletedConsumerTest {

    private HubDeletedEvent newEvent() {
        return new HubDeletedEvent(UUID.randomUUID(), Instant.now(), UUID.randomUUID());
    }

    @Test
    @DisplayName("Kafka 이벤트를 받으면 허브 소속 배송담당자 일괄 소프트삭제를 호출한다")
    void delegatesHubDeletionToAgentCommandService() {
        DeliveryAgentCommandService service = Mockito.mock(DeliveryAgentCommandService.class);
        HubDeletedConsumer consumer = new HubDeletedConsumer(service);
        HubDeletedEvent event = newEvent();

        consumer.consume(event);

        verify(service).deleteAllByHub(event.hubId(), event.deletedBy());
    }

    @Test
    @DisplayName("허브 소속 배송담당자 삭제 실패 예외를 Kafka Retry 처리기로 전달한다")
    void propagatesExceptionWhenDeletionFails() {
        DeliveryAgentCommandService service = Mockito.mock(DeliveryAgentCommandService.class);
        HubDeletedConsumer consumer = new HubDeletedConsumer(service);
        HubDeletedEvent event = newEvent();

        doThrow(new RuntimeException("hub agent deletion failed"))
            .when(service).deleteAllByHub(event.hubId(), event.deletedBy());

        assertThrows(RuntimeException.class, () -> consumer.consume(event));
    }

    @Test
    @DisplayName("최종 실패(DLT) 이벤트는 별도 후속 처리 없이 로깅만 한다")
    void dltHandlerDoesNotTriggerFurtherProcessing() {
        DeliveryAgentCommandService service = Mockito.mock(DeliveryAgentCommandService.class);
        HubDeletedConsumer consumer = new HubDeletedConsumer(service);
        HubDeletedEvent event = newEvent();

        consumer.handleDlt(event);

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Consumer의 재시도 횟수와 백오프 설정을 검증한다")
    void configuresRetryPolicy() throws NoSuchMethodException {
        Method consume = HubDeletedConsumer.class.getDeclaredMethod("consume", HubDeletedEvent.class);

        RetryableTopic retryableTopic = consume.getAnnotation(RetryableTopic.class);

        assertNotNull(retryableTopic);
        assertEquals("5", retryableTopic.attempts());
        assertEquals(5_000L, retryableTopic.backoff().delay());
        assertEquals(2.0, retryableTopic.backoff().multiplier());
    }

    @Test
    @DisplayName("최종 실패 이벤트를 처리하는 DLT Handler가 선언됐는지 검증한다")
    void configuresDltHandler() throws NoSuchMethodException {
        Method handleDlt = HubDeletedConsumer.class.getDeclaredMethod("handleDlt", HubDeletedEvent.class);

        assertNotNull(handleDlt.getAnnotation(DltHandler.class));
    }

    @Test
    @DisplayName("허브 삭제 처리 시 hubs 캐시에서 해당 허브 항목을 evict하도록 선언되어 있는지 검증한다")
    void configuresHubsCacheEviction() throws NoSuchMethodException {
        Method consume = HubDeletedConsumer.class.getDeclaredMethod("consume", HubDeletedEvent.class);

        CacheEvict cacheEvict = consume.getAnnotation(CacheEvict.class);

        assertNotNull(cacheEvict);
        assertArrayEquals(new String[] {"delivery-service-hubs"}, cacheEvict.value());
        assertEquals("#event.hubId()", cacheEvict.key());
    }
}