package com.logistics.delivery.infrastructure.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryAiNotificationOutboxServiceTest {

    @Mock DeliveryAiNotificationOutboxRepository repository;

    private DeliveryAiNotificationEvent newEvent(UUID deliveryId) {
        return new DeliveryAiNotificationEvent(
            UUID.randomUUID(), "v1", DeliveryAiNotificationEventType.DELIVERY_CREATED, Instant.now(),
            UUID.randomUUID(), deliveryId, UUID.randomUUID(), "slack-id",
            List.of(new DeliveryAiNotificationEvent.ProductItem("상품", 1)),
            "요청사항", "출발지", List.of("경유지"), "도착지", null, 30, 10);
    }

    @Test
    @DisplayName("이벤트를 직렬화해 아웃박스에 저장한다")
    void savesSerializedPayloadToOutbox() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        DeliveryAiNotificationOutboxService service = new DeliveryAiNotificationOutboxService(repository, objectMapper);
        UUID deliveryId = UUID.randomUUID();
        DeliveryAiNotificationEvent event = newEvent(deliveryId);

        service.save(event);

        ArgumentCaptor<DeliveryAiNotificationOutbox> captor = ArgumentCaptor.forClass(DeliveryAiNotificationOutbox.class);
        verify(repository).save(captor.capture());
        assertEquals(event.eventId(), captor.getValue().getEventId());
        assertEquals(deliveryId, captor.getValue().getDeliveryId());
        assertTrue(captor.getValue().getPayload().contains(deliveryId.toString()));
    }

    @Test
    @DisplayName("이벤트 직렬화에 실패하면 예외를 전파하지 않고 아웃박스 저장 자체를 건너뛴다")
    void skipsSavingWhenSerializationFails() throws JsonProcessingException {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        DeliveryAiNotificationOutboxService service = new DeliveryAiNotificationOutboxService(repository, objectMapper);
        doThrow(mock(JsonProcessingException.class)).when(objectMapper).writeValueAsString(any());

        service.save(newEvent(UUID.randomUUID()));

        verify(repository, never()).save(any());
    }
}