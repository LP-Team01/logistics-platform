package com.logistics.delivery.query.dto.request;

import com.logistics.delivery.domain.entity.AgentType;
import java.util.UUID;

public record DeliveryAgentSearchRequestDto(
    UUID hubId,
    AgentType agentType,
    Boolean  isAvailable
) {
}
