package com.logistics.delivery.command.dto.response;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "배송 담당자 등록 결과")
public record CreateDeliveryAgentResponseDto(
    @Schema(description = "배송 담당자 id")
    UUID agentId,
    @Schema(description = "소속 허브 id")
    UUID hubId,
    @Schema(description = "배송 담당자 유형")
    AgentType agentType,
    @Schema(description = "Slack id")
    String slackId,
    @Schema(description = "배정 순번(라운드로빈 기준)")
    Integer deliveryOrder,
    @Schema(description = "가용 여부")
    Boolean isAvailable,
    @Schema(description = "생성 일시")
    Instant createdAt
) {
    public static CreateDeliveryAgentResponseDto from(DeliveryAgent deliveryAgent) {
        return CreateDeliveryAgentResponseDto.builder()
            .agentId(deliveryAgent.getId())
            .hubId(deliveryAgent.getHubId())
            .agentType(deliveryAgent.getAgentType())
            .slackId(deliveryAgent.getSlackId())
            .deliveryOrder(deliveryAgent.getDeliveryOrder())
            .isAvailable(deliveryAgent.isAvailable())
            .createdAt(deliveryAgent.getCreatedAt())
            .build();
    }
}
