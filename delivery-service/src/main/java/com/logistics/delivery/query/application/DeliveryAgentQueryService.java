package com.logistics.delivery.query.application;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import com.logistics.delivery.domain.repository.DeliveryAgentSpecification;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.query.dto.reponse.DeliveryAgentDetailResponseDto;
import com.logistics.delivery.query.dto.reponse.DeliveryAgentResponseDto;
import com.logistics.delivery.query.dto.request.DeliveryAgentSearchRequestDto;
import java.util.Optional;
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
    private final DeliveryRouteRecordRepository deliveryRouteRecordRepository;
    private final CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;

    public DeliveryAgentDetailResponseDto getDeliveryAgent(UUID agentId) {
        DeliveryAgent deliveryAgent = findDeliveryAgent(agentId);
        return DeliveryAgentDetailResponseDto.from(deliveryAgent);
    }

    public DeliveryAgentResponseDto searchDeliveryAgents(DeliveryAgentSearchRequestDto request, Pageable pageable) {
        Specification<DeliveryAgent> spec = DeliveryAgentSpecification.withSearchCondition(
            request.hubId(), request.agentType(), request.isAvailable());
        Page<DeliveryAgent> page = deliveryAgentRepository.findAll(spec, pageable);
        return DeliveryAgentResponseDto.from(page);
    }

    public DeliveryAgentDetailResponseDto getNextDeliveryAgent(AgentType agentType, UUID hubId) {
        DeliveryAgent.validateHubId(agentType, hubId);
        Integer lastAssignedOrder = findLastAssignedOrder(agentType, hubId);
        DeliveryAgent nextAgent = findNextAvailableAgent(agentType, hubId, lastAssignedOrder);
        return DeliveryAgentDetailResponseDto.from(nextAgent);
    }

    private Integer findLastAssignedOrder(AgentType agentType, UUID hubId) {
        Optional<UUID> lastAssignedAgentId = Optional.empty();
        if (agentType == AgentType.HUB_DELIVERY) {
            lastAssignedAgentId = deliveryRouteRecordRepository.findFirstByAgentIdIsNotNullOrderByCreatedAtDesc()
                .map(DeliveryRouteRecord::getAgentId);
        }else if(agentType == AgentType.COMPANY_DELIVERY){
            lastAssignedAgentId = companyDeliveryRouteRecordRepository.findFirstByDepartureHubIdAndAgentIdIsNotNullOrderByCreatedAtDesc(hubId)
                .map(CompanyDeliveryRouteRecord::getAgentId);
        }
        return lastAssignedAgentId
            .flatMap(deliveryAgentRepository::findById)
            .map(DeliveryAgent::getDeliveryOrder)
            .orElse(null);
    }

    private DeliveryAgent findNextAvailableAgent(AgentType agentType, UUID hubId, Integer lastAssignedOrder) {
        if (lastAssignedOrder != null) {
            Optional<DeliveryAgent> afterCursor = deliveryAgentRepository
                .findFirstByAgentTypeAndHubIdAndIsAvailableTrueAndDeletedAtIsNullAndDeliveryOrderGreaterThanOrderByDeliveryOrderAsc(
                    agentType, hubId, lastAssignedOrder);
            if (afterCursor.isPresent()) {
                return afterCursor.get();
            }
        }
        // 커서 이후에 아무도 없으면(마지막 순번이었거나 첫 조회) 처음부터 다시 순환
        return deliveryAgentRepository
            .findFirstByAgentTypeAndHubIdAndIsAvailableTrueAndDeletedAtIsNullOrderByDeliveryOrderAsc(agentType, hubId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_PERSON_UNAVAILABLE));
    }

    private DeliveryAgent findDeliveryAgent(UUID agentId) {
        return deliveryAgentRepository.findByIdAndDeletedAtIsNull(agentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_PERSON_NOT_FOUND));
    }
}
