package com.logistics.delivery.infrastructure.kafka;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logistics.delivery.domain.entity.AgentType;
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
class DeliveryAgentApprovalOutboxPublisherTest {

    @Mock DeliveryAgentApprovalOutboxRepository repository;
    @Mock KafkaTemplate<String, DeliveryAgentApprovalResultEvent> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private DeliveryAgentApprovalOutboxPublisher newPublisher() {
        DeliveryAgentApprovalOutboxPublisher publisher =
            new DeliveryAgentApprovalOutboxPublisher(repository, kafkaTemplate, objectMapper);
        setTopic(publisher, "delivery-agent-approval-result");
        return publisher;
    }

    // @Value로 주입되는 topic 필드는 스프링 컨텍스트 없이는 채워지지 않아 리플렉션으로 직접 설정한다
    private void setTopic(DeliveryAgentApprovalOutboxPublisher publisher, String topic) {
        try {
            Field field = DeliveryAgentApprovalOutboxPublisher.class.getDeclaredField("topic");
            field.setAccessible(true);
            field.set(publisher, topic);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private DeliveryAgentApprovalResultEvent newEvent(UUID agentId) {
        return new DeliveryAgentApprovalResultEvent(
            UUID.randomUUID(), DeliveryAgentApprovalResultEventType.APPROVED, Instant.now(),
            agentId, UUID.randomUUID(), AgentType.HUB_DELIVERY, null, null);
    }

    private DeliveryAgentApprovalOutbox newOutbox(DeliveryAgentApprovalResultEvent event) throws Exception {
        return new DeliveryAgentApprovalOutbox(event.eventId(), event.agentId(), objectMapper.writeValueAsString(event));
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<SendResult<String, DeliveryAgentApprovalResultEvent>> successfulSend() {
        return CompletableFuture.completedFuture(mock(SendResult.class));
    }

    @Test
    @DisplayName("미발행 건을 Kafka로 전송하고 성공하면 발행 완료로 표시한다")
    void publishesPendingEventAndMarksAsPublished() throws Exception {
        DeliveryAgentApprovalResultEvent event = newEvent(UUID.randomUUID());
        DeliveryAgentApprovalOutbox outbox = newOutbox(event);
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(outbox));
        when(kafkaTemplate.send(eq("delivery-agent-approval-result"), eq(event.agentId().toString()), any()))
            .thenReturn(successfulSend());

        newPublisher().publishPending();

        assertNotNull(outbox.getPublishedAt());
    }

    @Test
    @DisplayName("Kafka 전송이 실패하면 예외를 삼키고 발행 완료로 표시하지 않는다")
    void keepsUnpublishedWhenKafkaSendFails() throws Exception {
        DeliveryAgentApprovalResultEvent event = newEvent(UUID.randomUUID());
        DeliveryAgentApprovalOutbox outbox = newOutbox(event);
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(outbox));
        CompletableFuture<SendResult<String, DeliveryAgentApprovalResultEvent>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("kafka down"));
        when(kafkaTemplate.send(eq("delivery-agent-approval-result"), eq(event.agentId().toString()), any()))
            .thenReturn(failed);

        assertDoesNotThrow(() -> newPublisher().publishPending());

        assertNull(outbox.getPublishedAt());
    }

    @Test
    @DisplayName("저장된 페이로드 역직렬화가 실패하면 발행을 시도하지 않고 발행 완료로 표시하지 않는다")
    void keepsUnpublishedWhenPayloadDeserializationFails() {
        DeliveryAgentApprovalOutbox outbox =
            new DeliveryAgentApprovalOutbox(UUID.randomUUID(), UUID.randomUUID(), "{invalid-json");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(outbox));

        assertDoesNotThrow(() -> newPublisher().publishPending());

        assertNull(outbox.getPublishedAt());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("여러 건 중 하나가 실패해도 나머지 건은 독립적으로 발행된다")
    void processesEachPendingRowIndependently() throws Exception {
        DeliveryAgentApprovalResultEvent okEvent = newEvent(UUID.randomUUID());
        DeliveryAgentApprovalOutbox okOutbox = newOutbox(okEvent);
        DeliveryAgentApprovalOutbox badOutbox =
            new DeliveryAgentApprovalOutbox(UUID.randomUUID(), UUID.randomUUID(), "{invalid-json");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(badOutbox, okOutbox));
        when(kafkaTemplate.send(eq("delivery-agent-approval-result"), eq(okEvent.agentId().toString()), any()))
            .thenReturn(successfulSend());

        newPublisher().publishPending();

        assertNull(badOutbox.getPublishedAt());
        assertNotNull(okOutbox.getPublishedAt());
    }
}