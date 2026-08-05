package com.logistics.delivery.command.dto.command;

import com.logistics.delivery.domain.entity.AgentType;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UpdateDeliveryAgentCommand(
    UUID hubId,
    String slackId,
    Boolean isAvailable,
    AgentType agentType
) {
}
