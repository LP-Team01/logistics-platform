package com.logistics.user.kafka;

import com.logistics.user.user.entity.AgentType;

import java.time.Instant;
import java.util.UUID;

// user-service가 배송담당자 승인(PENDING → APPROVING 커밋) 직후 발행하는 이벤트
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
