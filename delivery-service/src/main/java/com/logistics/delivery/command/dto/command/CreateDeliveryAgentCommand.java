package com.logistics.delivery.command.dto.command;

import com.logistics.delivery.domain.entity.AgentType;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateDeliveryAgentCommand(
    UUID agentId,
    UUID hubId,
    AgentType agentType,
    String slackId
) {
}
