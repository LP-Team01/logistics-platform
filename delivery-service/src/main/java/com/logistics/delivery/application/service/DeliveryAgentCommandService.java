package com.logistics.delivery.application.service;

import com.logistics.delivery.application.command.CreateDeliveryAgentCommand;
import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.presentation.dto.request.CreateDeliveryAgentRequest;
import com.logistics.delivery.presentation.dto.response.DeliveryAgentResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeliveryAgentCommandService {
    private final DeliveryAgentRepository deliveryAgentRepository;

    public DeliveryAgentResponse create(CreateDeliveryAgentCommand command) {
        // TODO: hub-service에 허브 존재 검증 API가 생기면 HubServiceClient로 hubId 유효성 확인 후 404 처리
        validateHubId(command.agentType(), command.hubId());

        DeliveryAgent deliveryAgent = DeliveryAgent.builder()
            .agentId(command.agentId())
            .hubId(command.hubId())
            .agentType(command.agentType())
            .slackId(command.slackId())
            .deliveryOrder(nextDeliveryOrder(command.agentType(), command.hubId()))
            .build();
        DeliveryAgent saved = deliveryAgentRepository.save(deliveryAgent);
        return DeliveryAgentResponse.from(saved);
    }

    private void validateHubId(AgentType agentType, UUID hubId) {
        if (agentType == AgentType.COMPANY_DELIVERY && hubId == null) {
            throw new BusinessException(ErrorCode.HUB_ID_REQUIRED);
        }
    }

    private int nextDeliveryOrder(AgentType agentType, UUID hubId) {
        return deliveryAgentRepository
            .findFirstByAgentTypeAndHubIdOrderByDeliveryOrderDesc(agentType, hubId)
            .map(agent -> agent.getDeliveryOrder() + 1)
            .orElse(0);
    }
}
