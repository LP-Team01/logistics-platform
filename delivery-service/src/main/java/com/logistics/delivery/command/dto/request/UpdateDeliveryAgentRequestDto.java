package com.logistics.delivery.command.dto.request;

import com.logistics.delivery.command.dto.UpdateDeliveryAgentCommand;
import com.logistics.delivery.domain.entity.AgentType;
import java.util.UUID;

public record UpdateDeliveryAgentRequestDto(

    UUID hubId,

    String slackId,

    Boolean isAvailable,

    AgentType agentType
) {
    public UpdateDeliveryAgentCommand toCommand() {
        return UpdateDeliveryAgentCommand.builder()
            .hubId(hubId)
            .slackId(slackId)
            .isAvailable(isAvailable)
            .agentType(agentType)
            .build();
    }
}
