package com.logistics.delivery.command.dto.response;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DeliveryAgentResponseDto(
    UUID agentId,
    UUID hubId,
    AgentType agentType,
    String slackId,
    Integer deliveryOrder,
    Boolean isAvailable,
    Instant createdAt
) {
    public static DeliveryAgentResponseDto from(DeliveryAgent deliveryAgent) {
        return DeliveryAgentResponseDto.builder()
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
