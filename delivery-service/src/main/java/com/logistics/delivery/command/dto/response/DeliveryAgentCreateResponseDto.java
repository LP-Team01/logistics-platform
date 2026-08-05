package com.logistics.delivery.command.dto.response;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DeliveryAgentCreateResponseDto(
    UUID agentId,
    UUID hubId,
    AgentType agentType,
    String slackId,
    Integer deliveryOrder,
    Boolean isAvailable,
    Instant createdAt
) {
    public static DeliveryAgentCreateResponseDto from(DeliveryAgent deliveryAgent) {
        return DeliveryAgentCreateResponseDto.builder()
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
