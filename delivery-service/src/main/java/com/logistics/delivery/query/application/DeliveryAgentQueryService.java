package com.logistics.delivery.query.application;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import com.logistics.delivery.domain.repository.DeliveryAgentSpecification;
import com.logistics.delivery.domain.service.DeliveryAgentAssignmentService;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.query.dto.response.DeliveryAgentDetailResponseDto;
import com.logistics.delivery.query.dto.response.DeliveryAgentResponseDto;
import com.logistics.delivery.query.dto.request.DeliveryAgentSearchRequestDto;
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
        DeliveryAgent nextAgent = deliveryAgentAssignmentService.assignNext(agentType, hubId);
        return DeliveryAgentDetailResponseDto.from(nextAgent);
    }

    private DeliveryAgent findDeliveryAgent(UUID agentId) {
        return deliveryAgentRepository.findByIdAndDeletedAtIsNull(agentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_PERSON_NOT_FOUND));
    }
}
