package com.logistics.delivery.command.application;

import com.logistics.delivery.command.dto.command.CreateDeliveryAgentCommand;
import com.logistics.delivery.command.dto.command.UpdateDeliveryAgentCommand;
import com.logistics.delivery.command.dto.response.CreateDeliveryAgentResponseDto;
import com.logistics.delivery.command.dto.response.UpdateDeliveryAgentResponseDto;
import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryAgentCommandService {
    private final DeliveryAgentRepository deliveryAgentRepository;

    private static final int MAX_COUNT = 10;
    private static final Set<UserRole> AGENT_MANAGE_ROLES = EnumSet.of(UserRole.MASTER, UserRole.HUB_MANAGER);

    @Transactional
    public CreateDeliveryAgentResponseDto create(CreateDeliveryAgentCommand command, UserRole userRole,
                                                  UUID requesterHubId) {
        // TODO: hub-service에 허브 존재 검증 API가 생기면 HubServiceClient로 hubId 유효성 확인 후 404 처리

        validateRole(userRole);
        validateHubManagerScope(userRole, command.agentType(), command.hubId(), requesterHubId);
        validateAgentCapacity(command.agentType(), command.hubId());
        DeliveryAgent deliveryAgent = DeliveryAgent.builder()
                .agentId(command.agentId())
                .hubId(command.hubId())
                .agentType(command.agentType())
                .slackId(command.slackId())
                .deliveryOrder(nextDeliveryOrder(command.agentType(), command.hubId()))
                .build();

        DeliveryAgent saved = deliveryAgentRepository.save(deliveryAgent);
        return CreateDeliveryAgentResponseDto.from(saved);
    }

    @Transactional
    public UpdateDeliveryAgentResponseDto update(UUID agentId, UpdateDeliveryAgentCommand command, UserRole userRole,
                                                  UUID requesterHubId) {
        validateRole(userRole);
        // TODO: hub-service에 허브 존재 검증 API가 생기면 hubId가 바뀌는 경우에도 HubServiceClient로 유효성 확인 필요
        DeliveryAgent deliveryAgent = findDeliveryAgent(agentId);
        validateHubManagerScope(userRole, deliveryAgent.getAgentType(), deliveryAgent.getHubId(), requesterHubId);
        AgentType effectiveAgentType = command.agentType() != null ? command.agentType() : deliveryAgent.getAgentType();
        UUID effectiveHubId = command.hubId() != null ? command.hubId() : deliveryAgent.getHubId();
        validateHubManagerScope(userRole, effectiveAgentType, effectiveHubId, requesterHubId);
        deliveryAgent.update(command.hubId(), command.agentType(), command.slackId(), command.isAvailable());
        return UpdateDeliveryAgentResponseDto.from(deliveryAgent);
    }

    @Transactional
    public void delete(UUID agentId, UUID requesterId, UserRole userRole, UUID requesterHubId) {
        validateRole(userRole);
        DeliveryAgent deliveryAgent = findDeliveryAgent(agentId);
        validateHubManagerScope(userRole, deliveryAgent.getAgentType(), deliveryAgent.getHubId(), requesterHubId);
        deliveryAgent.softDelete(requesterId);
    }

    private void validateAgentCapacity(AgentType agentType, UUID hubId) {
        int count = 0;
        if (agentType == AgentType.HUB_DELIVERY) {
            count = deliveryAgentRepository.countByAgentTypeAndDeletedAtIsNull(agentType);
        } else if (agentType == AgentType.COMPANY_DELIVERY) {
            count = deliveryAgentRepository.countByAgentTypeAndHubIdAndDeletedAtIsNull(agentType,hubId);
        }
        if (count >= MAX_COUNT) {
            throw new BusinessException(ErrorCode.DELIVERY_AGENT_LIMIT_EXCEEDED);
        }
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

    private void validateRole(UserRole userRole) {
        if (!AGENT_MANAGE_ROLES.contains(userRole)) {
            throw new BusinessException(ErrorCode.DELIVERY_AGENT_FORBIDDEN);
        }
    }

    private void validateHubManagerScope(UserRole userRole, AgentType agentType, UUID targetHubId,
                                          UUID requesterHubId) {
        if (userRole != UserRole.HUB_MANAGER) {
            return;
        }
        // 허브 배송 담당자는 특정 허브에 속하지 않는 시스템 전체 풀이라 HUB_MANAGER 관리 범위 밖(MASTER 전용)
        if (agentType == AgentType.HUB_DELIVERY) {
            throw new BusinessException(ErrorCode.DELIVERY_AGENT_FORBIDDEN);
        }
        if (requesterHubId == null || !requesterHubId.equals(targetHubId)) {
            throw new BusinessException(ErrorCode.DELIVERY_AGENT_FORBIDDEN);
        }
    }
}
