package com.logistics.delivery.infrastructure.kafka;

import com.logistics.delivery.domain.entity.AgentType;
import java.time.Instant;
import java.util.UUID;


 //user-service가 배송담당자 승인(PENDING → APPROVING 커밋) 직후 발행하는 이벤트
 //requesterId(실제 승인을 처리한 관리자)는 감사 기록(DeliveryAgent.createdBy)용 - null이면 감사 기록을 남기지 않음
public record DeliveryManagerApprovalRequestedEvent(
    UUID eventId,
    UUID agentId,
    UUID hubId,
    AgentType agentType,
    String slackId,
    UUID requesterId,
    Instant occurredAt
) {
}
