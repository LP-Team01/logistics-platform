package com.logistics.user.kafka;

import com.logistics.user.user.entity.AgentType;

import java.util.UUID;

public record DeliveryApprovalEvent(
    UUID requesterId,
    UUID requesterHubId,
    String requesterRole,
    UUID agentId,
    UUID hubId,
    AgentType agentType,
    String slackId
) {
}
