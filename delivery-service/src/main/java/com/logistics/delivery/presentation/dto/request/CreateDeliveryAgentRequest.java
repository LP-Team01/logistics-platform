package com.logistics.delivery.presentation.dto.request;

import com.logistics.delivery.application.command.CreateDeliveryAgentCommand;
import com.logistics.delivery.domain.entity.AgentType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDeliveryAgentRequest(
    @NotNull(message = "배송 담당자 Id는 필수입니다.")
    UUID agentId,

    UUID hubId,

    @NotNull(message = "배송 담당자 타입은 필수입니다.")
    AgentType agentType,

    String slackId
) {
    public CreateDeliveryAgentCommand toCommand() {
        return CreateDeliveryAgentCommand.builder()
            .agentId(agentId)
            .hubId(hubId)
            .agentType(agentType)
            .slackId(slackId)
            .build();
    }
}
