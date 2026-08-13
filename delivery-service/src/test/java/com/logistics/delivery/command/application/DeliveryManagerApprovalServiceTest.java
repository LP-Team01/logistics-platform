package com.logistics.delivery.command.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import com.logistics.delivery.global.common.AuditorContext;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.common.UserStatus;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.infrastructure.kafka.DeliveryAgentApprovalOutboxService;
import com.logistics.delivery.infrastructure.kafka.DeliveryAgentApprovalResultEvent;
import com.logistics.delivery.infrastructure.kafka.DeliveryAgentApprovalResultEventType;
import com.logistics.delivery.infrastructure.kafka.DeliveryManagerApprovalRequestedEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryManagerApprovalServiceTest {

    @Mock DeliveryAgentRepository deliveryAgentRepository;
    @Mock DeliveryAgentCommandService deliveryAgentCommandService;
    @Mock DeliveryAgentApprovalOutboxService outboxService;
    @InjectMocks DeliveryManagerApprovalService deliveryManagerApprovalService;

    private DeliveryManagerApprovalRequestedEvent newEvent(UUID agentId) {
        return new DeliveryManagerApprovalRequestedEvent(
            UUID.randomUUID(), agentId, UUID.randomUUID(),
            AgentType.HUB_DELIVERY, "slack-id", UUID.randomUUID(), Instant.now());
    }

    @Test
    @DisplayName("이미 생성된 배송담당자면 재생성 없이 승인 성공 이벤트만 재발행한다(Kafka 중복 소비 대응)")
    void republishesApprovedWithoutRecreatingWhenAgentAlreadyExists() {
        DeliveryManagerApprovalRequestedEvent event = newEvent(UUID.randomUUID());
        when(deliveryAgentRepository.findByIdAndDeletedAtIsNull(event.agentId()))
            .thenReturn(Optional.of(DeliveryAgent.builder()
                .agentId(event.agentId()).agentType(event.agentType()).deliveryOrder(0).build()));

        deliveryManagerApprovalService.handleApprovalRequested(event);

        verify(deliveryAgentCommandService, never()).create(any(), any(), any(), any());
        ArgumentCaptor<DeliveryAgentApprovalResultEvent> captor =
            ArgumentCaptor.forClass(DeliveryAgentApprovalResultEvent.class);
        verify(outboxService).save(captor.capture());
        assertEquals(DeliveryAgentApprovalResultEventType.APPROVED, captor.getValue().eventType());
        assertEquals(event.agentId(), captor.getValue().agentId());
    }

    @Test
    @DisplayName("신규 담당자 생성에 성공하면 승인 성공 이벤트를 발행한다")
    void createsAgentAndPublishesApprovedOnSuccess() {
        DeliveryManagerApprovalRequestedEvent event = newEvent(UUID.randomUUID());
        when(deliveryAgentRepository.findByIdAndDeletedAtIsNull(event.agentId())).thenReturn(Optional.empty());

        deliveryManagerApprovalService.handleApprovalRequested(event);

        verify(deliveryAgentCommandService).create(any(), eq(UserRole.MASTER), isNull(), eq(UserStatus.APPROVING));
        ArgumentCaptor<DeliveryAgentApprovalResultEvent> captor =
            ArgumentCaptor.forClass(DeliveryAgentApprovalResultEvent.class);
        verify(outboxService).save(captor.capture());
        assertEquals(DeliveryAgentApprovalResultEventType.APPROVED, captor.getValue().eventType());
        assertEquals(event.hubId(), captor.getValue().hubId());
    }

    @Test
    @DisplayName("담당자 생성이 실패(BusinessException)하면 실패 사유를 담아 승인 실패 이벤트를 발행한다")
    void publishesFailedWhenCreateThrowsBusinessException() {
        DeliveryManagerApprovalRequestedEvent event = newEvent(UUID.randomUUID());
        when(deliveryAgentRepository.findByIdAndDeletedAtIsNull(event.agentId())).thenReturn(Optional.empty());
        when(deliveryAgentCommandService.create(any(), any(), any(), any()))
            .thenThrow(new BusinessException(ErrorCode.DELIVERY_AGENT_LIMIT_EXCEEDED));

        deliveryManagerApprovalService.handleApprovalRequested(event);

        ArgumentCaptor<DeliveryAgentApprovalResultEvent> captor =
            ArgumentCaptor.forClass(DeliveryAgentApprovalResultEvent.class);
        verify(outboxService).save(captor.capture());
        assertEquals(DeliveryAgentApprovalResultEventType.APPROVAL_FAILED, captor.getValue().eventType());
        assertEquals(ErrorCode.DELIVERY_AGENT_LIMIT_EXCEEDED.name(), captor.getValue().failureReasonCode());
        assertEquals(ErrorCode.DELIVERY_AGENT_LIMIT_EXCEEDED.getMessage(), captor.getValue().failureReasonMessage());
    }

    @Test
    @DisplayName("생성 처리 중에는 요청자를 AuditorContext에 채우고, 끝나면 반드시 비운다")
    void setsAndClearsAuditorContextAroundCreation() {
        DeliveryManagerApprovalRequestedEvent event = newEvent(UUID.randomUUID());
        when(deliveryAgentRepository.findByIdAndDeletedAtIsNull(event.agentId())).thenReturn(Optional.empty());
        when(deliveryAgentCommandService.create(any(), any(), any(), any())).thenAnswer(invocation -> {
            assertTrue(AuditorContext.get().isPresent());
            assertEquals(event.requesterId(), AuditorContext.get().orElseThrow());
            return null;
        });

        deliveryManagerApprovalService.handleApprovalRequested(event);

        assertFalse(AuditorContext.get().isPresent());
    }

    @Test
    @DisplayName("재시도까지 모두 실패해 DLT로 넘어가면 고정된 실패 사유로 승인 실패 이벤트를 발행한다")
    void publishesFailedEventWithFixedReasonOnFinalDltFailure() {
        DeliveryManagerApprovalRequestedEvent event = newEvent(UUID.randomUUID());

        deliveryManagerApprovalService.handleApprovalRequestFailedFinally(event);

        ArgumentCaptor<DeliveryAgentApprovalResultEvent> captor =
            ArgumentCaptor.forClass(DeliveryAgentApprovalResultEvent.class);
        verify(outboxService).save(captor.capture());
        assertEquals(DeliveryAgentApprovalResultEventType.APPROVAL_FAILED, captor.getValue().eventType());
        assertEquals("DELIVERY_AGENT_APPROVAL_PROCESSING_FAILED", captor.getValue().failureReasonCode());
        assertEquals(event.agentId(), captor.getValue().agentId());
    }
}