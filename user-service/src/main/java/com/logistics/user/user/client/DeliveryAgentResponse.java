package com.logistics.user.user.client;

import com.logistics.user.user.entity.AgentType;
import java.time.Instant;
import java.util.UUID;

public record DeliveryAgentResponse(
    UUID agentId,
    UUID hubId,
    AgentType agentType,
    String slackId,
    Integer deliveryOrder,
    Boolean isAvailable,
    Instant createdAt
) {
}
