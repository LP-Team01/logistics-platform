package com.logistics.user.user.client;

import com.logistics.user.user.entity.AgentType;
import java.util.UUID;

public record CreateDeliveryAgentRequest(
    UUID agentId,
    UUID hubId,
    AgentType agentType,
    String slackId
) {
}
