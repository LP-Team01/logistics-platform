package com.logistics.delivery.infrastructure.kafka;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class DeliveryAiNotificationOutboxPublisherTest {

    @Mock DeliveryAiNotificationOutboxRepository repository;
    @Mock KafkaTemplate<String, DeliveryAiNotificationEvent> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private DeliveryAiNotificationOutboxPublisher newPublisher() {
        DeliveryAiNotificationOutboxPublisher publisher =
            new DeliveryAiNotificationOutboxPublisher(repository, kafkaTemplate, objectMapper);
        setTopic(publisher, "delivery-ai-notification");
        return publisher;
    }

    // @Value로 주입되는 topic 필드는 스프링 컨텍스트 없이는 채워지지 않아 리플렉션으로 직접 설정한다
    private void setTopic(DeliveryAiNotificationOutboxPublisher publisher, String topic) {
        try {
            Field field = DeliveryAiNotificationOutboxPublisher.class.getDeclaredField("topic");
            field.setAccessible(true);
            field.set(publisher, topic);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private DeliveryAiNotificationEvent newEvent(UUID deliveryId) {
        return new DeliveryAiNotificationEvent(
            UUID.randomUUID(), "v1", DeliveryAiNotificationEventType.DELIVERY_CREATED, Instant.now(),
            UUID.randomUUID(), deliveryId, UUID.randomUUID(), "slack-id",
            List.of(new DeliveryAiNotificationEvent.ProductItem("상품", 1)),
            "요청사항", "출발지", List.of("경유지"), "도착지", null, 30, 10);
    }

    private DeliveryAiNotificationOutbox newOutbox(DeliveryAiNotificationEvent event) throws Exception {
        return new DeliveryAiNotificationOutbox(event.eventId(), event.deliveryId(), objectMapper.writeValueAsString(event));
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<SendResult<String, DeliveryAiNotificationEvent>> successfulSend() {
        return CompletableFuture.completedFuture(mock(SendResult.class));
    }

    @Test
    @DisplayName("미발행 건을 Kafka로 전송하고 성공하면 발행 완료로 표시한다")
    void publishesPendingEventAndMarksAsPublished() throws Exception {
        DeliveryAiNotificationEvent event = newEvent(UUID.randomUUID());
        DeliveryAiNotificationOutbox outbox = newOutbox(event);
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(outbox));
        when(kafkaTemplate.send(eq("delivery-ai-notification"), eq(event.deliveryId().toString()), any()))
            .thenReturn(successfulSend());

        newPublisher().publishPending();

        assertNotNull(outbox.getPublishedAt());
    }

    @Test
    @DisplayName("Kafka 전송이 실패하면 예외를 삼키고 발행 완료로 표시하지 않는다")
    void keepsUnpublishedWhenKafkaSendFails() throws Exception {
        DeliveryAiNotificationEvent event = newEvent(UUID.randomUUID());
        DeliveryAiNotificationOutbox outbox = newOutbox(event);
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(outbox));
        CompletableFuture<SendResult<String, DeliveryAiNotificationEvent>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("kafka down"));
        when(kafkaTemplate.send(eq("delivery-ai-notification"), eq(event.deliveryId().toString()), any()))
            .thenReturn(failed);

        assertDoesNotThrow(() -> newPublisher().publishPending());

        assertNull(outbox.getPublishedAt());
    }

    @Test
    @DisplayName("저장된 페이로드 역직렬화가 실패하면 발행을 시도하지 않고 발행 완료로 표시하지 않는다")
    void keepsUnpublishedWhenPayloadDeserializationFails() {
        DeliveryAiNotificationOutbox outbox =
            new DeliveryAiNotificationOutbox(UUID.randomUUID(), UUID.randomUUID(), "{invalid-json");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(outbox));

        assertDoesNotThrow(() -> newPublisher().publishPending());

        assertNull(outbox.getPublishedAt());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("여러 건 중 하나가 실패해도 나머지 건은 독립적으로 발행된다")
    void processesEachPendingRowIndependently() throws Exception {
        DeliveryAiNotificationEvent okEvent = newEvent(UUID.randomUUID());
        DeliveryAiNotificationOutbox okOutbox = newOutbox(okEvent);
        DeliveryAiNotificationOutbox badOutbox =
            new DeliveryAiNotificationOutbox(UUID.randomUUID(), UUID.randomUUID(), "{invalid-json");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(badOutbox, okOutbox));
        when(kafkaTemplate.send(eq("delivery-ai-notification"), eq(okEvent.deliveryId().toString()), any()))
            .thenReturn(successfulSend());

        newPublisher().publishPending();

        assertNull(badOutbox.getPublishedAt());
        assertNotNull(okOutbox.getPublishedAt());
    }
}