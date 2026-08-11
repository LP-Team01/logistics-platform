package com.logistics.user.kafka.consumer;

import com.logistics.user.user.entity.AgentType;

import java.time.Instant;
import java.util.UUID;

// delivery-service가 유저 승인 사가의 결과로 user-service에 발행하는 이벤트
public record DeliveryAgentApprovalResultEvent(
    UUID eventId,
    DeliveryAgentApprovalResultEventType eventType,
    Instant occurredAt,
    UUID agentId,
    UUID hubId,
    AgentType agentType,
    String failureReasonCode,
    String failureReasonMessage
) {
}
