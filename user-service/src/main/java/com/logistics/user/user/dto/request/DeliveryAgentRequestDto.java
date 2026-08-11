package com.logistics.user.user.dto.request;

import com.logistics.user.user.entity.AgentType;

import java.util.UUID;

public record DeliveryAgentRequestDto(
    AgentType agentType
) {
}
