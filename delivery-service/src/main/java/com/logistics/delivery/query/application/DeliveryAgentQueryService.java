package com.logistics.delivery.query.application;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import com.logistics.delivery.domain.repository.DeliveryAgentSpecification;
import com.logistics.delivery.domain.service.DeliveryAgentAssignmentService;
import com.logistics.delivery.global.common.DeliveryAccessGuard;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.query.dto.response.DeliveryAgentDetailResponseDto;
import com.logistics.delivery.query.dto.response.DeliveryAgentResponseDto;
import com.logistics.delivery.query.dto.request.DeliveryAgentSearchRequestDto;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryAgentQueryService {
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final DeliveryAgentAssignmentService deliveryAgentAssignmentService;

    private static final Set<UserRole> AGENT_QUERY_ROLES =
        EnumSet.of(UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER);

    public DeliveryAgentDetailResponseDto getDeliveryAgent(UserRole userRole, UUID requesterId, UUID requesterHubId,
                                                            UUID agentId) {
        DeliveryAgent deliveryAgent = findDeliveryAgent(agentId);
        validateAgentAccess(userRole, requesterId, requesterHubId, deliveryAgent);
        return DeliveryAgentDetailResponseDto.from(deliveryAgent);
    }

    public DeliveryAgentResponseDto searchDeliveryAgents(UserRole userRole, UUID requesterId, UUID requesterHubId,
                                                         DeliveryAgentSearchRequestDto request, Pageable pageable) {
        DeliveryAccessGuard.requireRole(userRole, AGENT_QUERY_ROLES, ErrorCode.DELIVERY_AGENT_QUERY_FORBIDDEN);
        UUID agentId = null;
        UUID hubId = request.hubId();
        if (userRole == UserRole.DELIVERY_MANAGER) {
            agentId = requesterId;
        } else if (userRole == UserRole.HUB_MANAGER) {
            DeliveryAccessGuard.requireNonNull(requesterHubId, ErrorCode.DELIVERY_AGENT_QUERY_FORBIDDEN);
            hubId = requesterHubId;
        }
        Specification<DeliveryAgent> spec = DeliveryAgentSpecification.withSearchCondition(
            hubId, request.agentType(), request.isAvailable(), agentId);
        Page<DeliveryAgent> page = deliveryAgentRepository.findAll(spec, pageable);
        return DeliveryAgentResponseDto.from(page);
    }

    private void validateAgentAccess(UserRole userRole, UUID requesterId, UUID requesterHubId,
                                      DeliveryAgent deliveryAgent) {
        DeliveryAccessGuard.requireRole(userRole, AGENT_QUERY_ROLES, ErrorCode.DELIVERY_AGENT_QUERY_FORBIDDEN);
        if (userRole == UserRole.DELIVERY_MANAGER) {
            DeliveryAccessGuard.requireOwnAgent(requesterId, deliveryAgent.getId(),
                ErrorCode.DELIVERY_AGENT_QUERY_FORBIDDEN);
        }
        if (userRole == UserRole.HUB_MANAGER) {
            if (deliveryAgent.getAgentType() == AgentType.HUB_DELIVERY) {
                throw new BusinessException(ErrorCode.DELIVERY_AGENT_QUERY_FORBIDDEN);
            }
            DeliveryAccessGuard.requireWithinHub(requesterHubId, ErrorCode.DELIVERY_AGENT_QUERY_FORBIDDEN,
                deliveryAgent.getHubId());
        }
    }

    public DeliveryAgentDetailResponseDto getNextDeliveryAgent(AgentType agentType, UUID hubId) {
        DeliveryAgent nextAgent = deliveryAgentAssignmentService.assignNext(agentType, hubId);
        return DeliveryAgentDetailResponseDto.from(nextAgent);
    }

    private DeliveryAgent findDeliveryAgent(UUID agentId) {
        return deliveryAgentRepository.findByIdAndDeletedAtIsNull(agentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_PERSON_NOT_FOUND));
    }
}
