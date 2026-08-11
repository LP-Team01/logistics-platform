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
import com.logistics.delivery.domain.entity.AgentType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryAgentApprovalOutboxServiceTest {

    @Mock DeliveryAgentApprovalOutboxRepository repository;

    private DeliveryAgentApprovalResultEvent newEvent(UUID agentId) {
        return new DeliveryAgentApprovalResultEvent(
            UUID.randomUUID(), DeliveryAgentApprovalResultEventType.APPROVED, Instant.now(),
            agentId, UUID.randomUUID(), AgentType.HUB_DELIVERY, null, null);
    }

    @Test
    @DisplayName("승인 결과 이벤트를 직렬화해 아웃박스에 저장한다")
    void savesSerializedPayloadToOutbox() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        DeliveryAgentApprovalOutboxService service = new DeliveryAgentApprovalOutboxService(repository, objectMapper);
        UUID agentId = UUID.randomUUID();
        DeliveryAgentApprovalResultEvent event = newEvent(agentId);

        service.save(event);

        ArgumentCaptor<DeliveryAgentApprovalOutbox> captor = ArgumentCaptor.forClass(DeliveryAgentApprovalOutbox.class);
        verify(repository).save(captor.capture());
        assertEquals(event.eventId(), captor.getValue().getEventId());
        assertEquals(agentId, captor.getValue().getAgentId());
        assertTrue(captor.getValue().getPayload().contains("APPROVED"));
    }

    @Test
    @DisplayName("승인 결과 이벤트 직렬화에 실패하면 예외를 전파하지 않고 아웃박스 저장 자체를 건너뛴다")
    void skipsSavingWhenSerializationFails() throws JsonProcessingException {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        DeliveryAgentApprovalOutboxService service = new DeliveryAgentApprovalOutboxService(repository, objectMapper);
        doThrow(mock(JsonProcessingException.class)).when(objectMapper).writeValueAsString(any());

        service.save(newEvent(UUID.randomUUID()));

        verify(repository, never()).save(any());
    }
}