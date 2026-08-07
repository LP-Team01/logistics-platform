package com.logistics.delivery.query.dto.response;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record DeliveryAgentResponseDto(
    List<DeliverAgentSummary> content,
    Long totalElements,
    Integer totalPages,
    Integer size,
    Integer number
) {

    public record DeliverAgentSummary(
        UUID agentId,
        UUID hubId,
        AgentType agentType,
        String slackId,
        Integer deliveryOrder,
        Boolean isAvailable
    ) {
        public static DeliverAgentSummary from(DeliveryAgent deliveryAgent) {
            return new DeliverAgentSummary(
                deliveryAgent.getId(),
                deliveryAgent.getHubId(),
                deliveryAgent.getAgentType(),
                deliveryAgent.getSlackId(),
                deliveryAgent.getDeliveryOrder(),
                deliveryAgent.isAvailable()
            );
        }
    }

    public static DeliveryAgentResponseDto from(Page<DeliveryAgent> page) {
        return DeliveryAgentResponseDto.builder()
            .content(page.getContent().stream().map(DeliverAgentSummary::from).toList())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .size(page.getSize())
            .number(page.getNumber())
            .build();
    }
}
