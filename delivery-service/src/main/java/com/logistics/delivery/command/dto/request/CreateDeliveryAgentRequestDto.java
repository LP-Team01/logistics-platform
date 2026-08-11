package com.logistics.delivery.command.dto.request;

import com.logistics.delivery.command.dto.command.CreateDeliveryAgentCommand;
import com.logistics.delivery.domain.entity.AgentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "배송 담당자 등록 요청")
public record CreateDeliveryAgentRequestDto(
    @Schema(description = "배송 담당자로 등록할 사용자 id(User 서비스 사용자 id와 동일)")
    @NotNull(message = "배송 담당자 Id는 필수입니다.")
    UUID agentId,

    @Schema(description = "소속 허브 id(agentType=COMPANY_DELIVERY일 때 필수)")
    UUID hubId,

    @Schema(description = "배송 담당자 유형")
    @NotNull(message = "배송 담당자 타입은 필수입니다.")
    AgentType agentType,

    @Schema(description = "Slack id")
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
