package com.logistics.delivery.command.dto.request;

import com.logistics.delivery.command.dto.CreateDeliveryAgentCommand;
import com.logistics.delivery.domain.entity.AgentType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDeliveryAgentRequestDto(
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
