package com.logistics.delivery.domain.service;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 배송담당자 순번(deliveryOrder) 기준 라운드로빈 배정. 동시성 제어(락)는 이후 별도 작업.
@Service
@RequiredArgsConstructor
public class DeliveryAgentAssignmentService {

    private final DeliveryAgentRepository deliveryAgentRepository;
    private final DeliveryRouteRecordRepository deliveryRouteRecordRepository;
    private final CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;

    public DeliveryAgent assignNext(AgentType agentType, UUID hubId) {
        DeliveryAgent.validateHubId(agentType, hubId);
        Integer lastAssignedOrder = findLastAssignedOrder(agentType, hubId);
        return findNextAvailableAgent(agentType, hubId, lastAssignedOrder);
    }

    private Integer findLastAssignedOrder(AgentType agentType, UUID hubId) {
        Optional<UUID> lastAssignedAgentId = Optional.empty();
        if (agentType == AgentType.HUB_DELIVERY) {
            lastAssignedAgentId = deliveryRouteRecordRepository.findFirstByAgentIdIsNotNullOrderByCreatedAtDesc()
                .map(DeliveryRouteRecord::getAgentId);
        } else if (agentType == AgentType.COMPANY_DELIVERY) {
            lastAssignedAgentId = companyDeliveryRouteRecordRepository
                .findFirstByDepartureHubIdAndAgentIdIsNotNullOrderByCreatedAtDesc(hubId)
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
        // 커서 이후에 아무도 없으면(마지막 순번이었거나 첫 배정) 처음부터 다시 순환
        return deliveryAgentRepository
            .findFirstByAgentTypeAndHubIdAndIsAvailableTrueAndDeletedAtIsNullOrderByDeliveryOrderAsc(agentType, hubId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_PERSON_UNAVAILABLE));
    }
}