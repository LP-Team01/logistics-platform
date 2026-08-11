package com.logistics.delivery.command.application;

import com.logistics.delivery.command.dto.command.CreateDeliveryAgentCommand;
import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import com.logistics.delivery.global.common.AuditorContext;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.common.UserStatus;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.infrastructure.kafka.DeliveryAgentApprovalOutboxService;
import com.logistics.delivery.infrastructure.kafka.DeliveryAgentApprovalResultEvent;
import com.logistics.delivery.infrastructure.kafka.DeliveryAgentApprovalResultEventType;
import com.logistics.delivery.infrastructure.kafka.DeliveryManagerApprovalRequestedEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryManagerApprovalService {

    private static final String DLT_FAILURE_CODE = "DELIVERY_AGENT_APPROVAL_PROCESSING_FAILED";
    private static final String DLT_FAILURE_MESSAGE = "배송담당자 승인 처리가 반복 실패해 최종 실패로 처리되었습니다.";

    private final DeliveryAgentRepository deliveryAgentRepository;
    private final DeliveryAgentCommandService deliveryAgentCommandService;
    private final DeliveryAgentApprovalOutboxService outboxService;

    @Transactional
    public void handleApprovalRequested(DeliveryManagerApprovalRequestedEvent event) {
        // Kafka는 at-least-once라 같은 요청이 중복 소비될 수 있음 - 이미 생성된 담당자면 재생성 없이 성공만 재발행
        if (deliveryAgentRepository.findByIdAndDeletedAtIsNull(event.agentId()).isPresent()) {
            log.info("이미 생성된 배송담당자입니다 - 재생성 없이 승인 성공 이벤트만 재발행. agentId={}", event.agentId());
            publishApproved(event.agentId(), event.hubId(), event.agentType());
            return;
        }

        CreateDeliveryAgentCommand command = CreateDeliveryAgentCommand.builder()
            .agentId(event.agentId())
            .hubId(event.hubId())
            .agentType(event.agentType())
            .slackId(event.slackId())
            .build();

        try {
            // Kafka 컨슈머 스레드에는 HTTP 요청이 없어 AuditorAware가 X-User-Id를 못 읽으므로,
            // 실제 승인 처리자(requesterId)를 AuditorContext에 명시적으로 넣어 createdBy가 채워지게 한다.
            // requesterId가 없으면(user-service 쪽 미지원) createdBy는 null로 남는다.
            AuditorContext.set(event.requesterId());
            // user-service가 이미 승인을 커밋한 뒤 들어오는 요청이므로 시스템 권한(MASTER)으로 기존 create()를
            // 그대로 재사용 - REST 경로(허브관리자 직접 호출)와 역할/정원 검증 로직을 이원화하지 않기 위함.
            // 다만 이 시점의 user-service 상태는 아직 APPROVED가 아니라 APPROVING(중간 상태)이므로 그에 맞춰 검증한다 -
            // delivery-service의 이 성공 이벤트를 받아야 user-service가 비로소 APPROVING -> APPROVED로 전환됨.
            deliveryAgentCommandService.create(command, UserRole.MASTER, null, UserStatus.APPROVING);
            publishApproved(event.agentId(), event.hubId(), event.agentType());
        } catch (BusinessException exception) {
            log.warn("배송담당자 생성 실패로 승인 실패 이벤트를 발행합니다. agentId={}, reason={}",
                event.agentId(), exception.getErrorCode(), exception);
            publishFailed(event.agentId(), event.hubId(), event.agentType(),
                exception.getErrorCode().name(), exception.getErrorCode().getMessage());
        } finally {
            AuditorContext.clear();
        }
    }

    //재시도(5회)까지 모두 실패해 DLT로 넘어간 경우 호출
    @Transactional
    public void handleApprovalRequestFailedFinally(DeliveryManagerApprovalRequestedEvent event) {
        publishFailed(event.agentId(), event.hubId(), event.agentType(), DLT_FAILURE_CODE, DLT_FAILURE_MESSAGE);
    }

    private void publishApproved(UUID agentId, UUID hubId, AgentType agentType) {
        outboxService.save(new DeliveryAgentApprovalResultEvent(
            UUID.randomUUID(), DeliveryAgentApprovalResultEventType.APPROVED, Instant.now(),
            agentId, hubId, agentType, null, null));
    }

    private void publishFailed(UUID agentId, UUID hubId, AgentType agentType, String reasonCode, String reasonMessage) {
        outboxService.save(new DeliveryAgentApprovalResultEvent(
            UUID.randomUUID(), DeliveryAgentApprovalResultEventType.APPROVAL_FAILED, Instant.now(),
            agentId, hubId, agentType, reasonCode, reasonMessage));
    }
}
