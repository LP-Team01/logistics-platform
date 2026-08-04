package com.logistics.delivery.command.application;

import com.logistics.delivery.command.dto.command.CreateDeliveryAgentCommand;
import com.logistics.delivery.command.dto.command.UpdateDeliveryAgentCommand;
import com.logistics.delivery.command.dto.response.DeliveryAgentCreateResponseDto;
import com.logistics.delivery.command.dto.response.DeliveryAgentUpdateResponseDto;
import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryAgentCommandService {
    private final DeliveryAgentRepository deliveryAgentRepository;

    @Transactional
    public DeliveryAgentCreateResponseDto create(CreateDeliveryAgentCommand command) {
        // TODO: hub-service에 허브 존재 검증 API가 생기면 HubServiceClient로 hubId 유효성 확인 후 404 처리
        DeliveryAgent deliveryAgent = DeliveryAgent.builder()
                .agentId(command.agentId())
                .hubId(command.hubId())
                .agentType(command.agentType())
                .slackId(command.slackId())
                .deliveryOrder(nextDeliveryOrder(command.agentType(), command.hubId()))
                .build();
        DeliveryAgent saved = deliveryAgentRepository.save(deliveryAgent);
        return DeliveryAgentCreateResponseDto.from(saved);
    }

    @Transactional
    public DeliveryAgentUpdateResponseDto update(UUID agentId, UpdateDeliveryAgentCommand command) {
        // TODO: hub-service에 허브 존재 검증 API가 생기면 hubId가 바뀌는 경우에도 HubServiceClient로 유효성 확인 필요
        DeliveryAgent deliveryAgent = findDeliveryAgent(agentId);
        deliveryAgent.update(command.hubId(), command.agentType(), command.slackId(), command.isAvailable());
        return DeliveryAgentUpdateResponseDto.from(deliveryAgent);
    }

    @Transactional
    public void delete(UUID agentId, UUID requesterId) {
        DeliveryAgent deliveryAgent = findDeliveryAgent(agentId);
        deliveryAgent.softDelete(requesterId);
    }

    private DeliveryAgent findDeliveryAgent(UUID agentId) {
        return deliveryAgentRepository.findByIdAndDeletedAtIsNull(agentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_PERSON_NOT_FOUND));
    }

    private int nextDeliveryOrder(AgentType agentType, UUID hubId) {
        return deliveryAgentRepository
                .findFirstByAgentTypeAndHubIdAndDeletedAtIsNullOrderByDeliveryOrderDesc(agentType, hubId)
                .map(agent -> agent.getDeliveryOrder() + 1)
                .orElse(0);
    }
}
