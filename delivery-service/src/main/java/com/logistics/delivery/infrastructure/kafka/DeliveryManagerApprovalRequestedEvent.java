package com.logistics.delivery.infrastructure.kafka;

import com.logistics.delivery.domain.entity.AgentType;
import java.time.Instant;
import java.util.UUID;


 //user-service가 배송담당자 승인(PENDING → APPROVING 커밋) 직후 발행하는 이벤트
public record DeliveryManagerApprovalRequestedEvent(
    UUID eventId,
    UUID agentId,
    UUID hubId,
    AgentType agentType,
    String slackId,
    Instant occurredAt
) {
}
