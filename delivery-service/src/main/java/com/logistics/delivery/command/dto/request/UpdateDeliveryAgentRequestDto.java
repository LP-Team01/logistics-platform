package com.logistics.delivery.command.dto.request;

import com.logistics.delivery.command.dto.command.UpdateDeliveryAgentCommand;
import com.logistics.delivery.domain.entity.AgentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "배송 담당자 수정 요청")
public record UpdateDeliveryAgentRequestDto(

    @Schema(description = "소속 허브 id")
    UUID hubId,

    @Schema(description = "Slack id")
    String slackId,

    @Schema(description = "가용 여부")
    Boolean isAvailable,

    @Schema(description = "배송 담당자 유형")
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
