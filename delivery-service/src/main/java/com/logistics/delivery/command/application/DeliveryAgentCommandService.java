package com.logistics.delivery.command.application;

import com.logistics.delivery.command.dto.command.CreateDeliveryAgentCommand;
import com.logistics.delivery.command.dto.command.UpdateDeliveryAgentCommand;
import com.logistics.delivery.command.dto.response.CreateDeliveryAgentResponseDto;
import com.logistics.delivery.command.dto.response.UpdateDeliveryAgentResponseDto;
import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import com.logistics.delivery.global.common.DeliveryAccessGuard;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.common.UserStatus;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.infrastructure.client.HubServiceClient;
import com.logistics.delivery.infrastructure.client.UserServiceClient;
import com.logistics.delivery.infrastructure.client.dto.UserServiceUserResponseDto;
import feign.FeignException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryAgentCommandService {
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final UserServiceClient userServiceClient;
    private final HubServiceClient hubServiceClient;

    private static final int MAX_COUNT = 10;
    private static final Set<UserRole> AGENT_MANAGE_ROLES = EnumSet.of(UserRole.MASTER, UserRole.HUB_MANAGER);

    @Transactional
    public CreateDeliveryAgentResponseDto create(CreateDeliveryAgentCommand command, UserRole userRole,
                                                  UUID requesterHubId) {
        validateRole(userRole);
        validateHubManagerScope(userRole, command.agentType(), command.hubId(), requesterHubId);
        validateHubExists(command.hubId());
        validateAgentUser(command.agentId());

        // (agentType, hubId) 그룹 자체를 advisory lock으로 먼저 잠근다 - "빈 그룹 최초 등록" 동시 요청까지 직렬화하기 위함
        deliveryAgentRepository.lockAgentGroup(groupLockKey(command.agentType(), command.hubId()));

        // (agentType, hubId) 그룹을 잠근 뒤 정원 검증과 배송순번 채번을 한 트랜잭션 안에서 처리->동시 생성 요청으로 인한 정원 초과·순번 중복을 막기
        List<DeliveryAgent> lockedAgents = deliveryAgentRepository
            .findByAgentTypeAndHubIdAndDeletedAtIsNullOrderByDeliveryOrderAsc(command.agentType(), command.hubId());
        validateAgentCapacity(lockedAgents);

        DeliveryAgent deliveryAgent = DeliveryAgent.builder()
                .agentId(command.agentId())
                .hubId(command.hubId())
                .agentType(command.agentType())
                .slackId(command.slackId())
                .deliveryOrder(nextDeliveryOrder(lockedAgents))
                .build();

        DeliveryAgent saved = deliveryAgentRepository.save(deliveryAgent);
        return CreateDeliveryAgentResponseDto.from(saved);
    }

    @Transactional
    public UpdateDeliveryAgentResponseDto update(UUID agentId, UpdateDeliveryAgentCommand command, UserRole userRole,
                                                  UUID requesterHubId) {
        validateRole(userRole);
        DeliveryAgent deliveryAgent = findDeliveryAgent(agentId);
        validateHubManagerScope(userRole, deliveryAgent.getAgentType(), deliveryAgent.getHubId(), requesterHubId);
        validateNoGroupChange(deliveryAgent, command.agentType(), command.hubId());
        deliveryAgent.update(command.slackId(), command.isAvailable());
        return UpdateDeliveryAgentResponseDto.from(deliveryAgent);
    }

    @Transactional
    public void delete(UUID agentId, UUID requesterId, UserRole userRole, UUID requesterHubId) {
        validateRole(userRole);
        DeliveryAgent deliveryAgent = findDeliveryAgent(agentId);
        validateHubManagerScope(userRole, deliveryAgent.getAgentType(), deliveryAgent.getHubId(), requesterHubId);
        deliveryAgent.softDelete(requesterId);
    }

    private void validateHubExists(UUID hubId) {
        if (hubId == null) {
            return;
        }
        try {
            hubServiceClient.getHub(hubId);
        } catch (FeignException.NotFound e) {
            throw new BusinessException(ErrorCode.DELIVERY_AGENT_HUB_NOT_FOUND);
        }
    }

    private String groupLockKey(AgentType agentType, UUID hubId) {
        return agentType + ":" + (hubId != null ? hubId : "GLOBAL");
    }

    // agentType/hubId 변경은 그룹(정원·순번) 재계산이 필요해 update()로 지원하지 않음 -> 그룹을 옮기려면 삭제 후 재등록해야 함
    private void validateNoGroupChange(DeliveryAgent deliveryAgent, AgentType requestedAgentType, UUID requestedHubId) {
        if (requestedAgentType != null && requestedAgentType != deliveryAgent.getAgentType()) {
            throw new BusinessException(ErrorCode.DELIVERY_AGENT_GROUP_CHANGE_NOT_ALLOWED);
        }
        if (requestedHubId != null && !requestedHubId.equals(deliveryAgent.getHubId())) {
            throw new BusinessException(ErrorCode.DELIVERY_AGENT_GROUP_CHANGE_NOT_ALLOWED);
        }
    }

    private void validateAgentUser(UUID agentId) {
        UserServiceUserResponseDto user;
        try {
            user = userServiceClient.getUser(agentId);
        } catch (FeignException.NotFound e) {
            throw new BusinessException(ErrorCode.DELIVERY_AGENT_USER_NOT_FOUND);
        }
        if (user.role() != UserRole.DELIVERY_MANAGER) {
            throw new BusinessException(ErrorCode.DELIVERY_AGENT_INVALID_USER_ROLE);
        }
        if (user.status() != UserStatus.APPROVED) {
            throw new BusinessException(ErrorCode.DELIVERY_AGENT_USER_NOT_APPROVED);
        }
    }

    private void validateAgentCapacity(List<DeliveryAgent> lockedAgents) {
        if (lockedAgents.size() >= MAX_COUNT) {
            throw new BusinessException(ErrorCode.DELIVERY_AGENT_LIMIT_EXCEEDED);
        }
    }

    private DeliveryAgent findDeliveryAgent(UUID agentId) {
        return deliveryAgentRepository.findByIdAndDeletedAtIsNull(agentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_PERSON_NOT_FOUND));
    }

    private int nextDeliveryOrder(List<DeliveryAgent> lockedAgents) {
        return lockedAgents.isEmpty()
            ? 0
            : lockedAgents.get(lockedAgents.size() - 1).getDeliveryOrder() + 1;
    }

    private void validateRole(UserRole userRole) {
        DeliveryAccessGuard.requireRole(userRole, AGENT_MANAGE_ROLES, ErrorCode.DELIVERY_AGENT_FORBIDDEN);
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
        DeliveryAccessGuard.requireWithinHub(requesterHubId, ErrorCode.DELIVERY_AGENT_FORBIDDEN, targetHubId);
    }
}
