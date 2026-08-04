package com.logistics.delivery.query.application;

import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.query.dto.reponse.DeliveryAgentDetailResponseDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryAgentQueryService {
    private final DeliveryAgentRepository deliveryAgentRepository;

    public DeliveryAgentDetailResponseDto getDeliveryAgent(UUID agentId) {
        DeliveryAgent deliveryAgent = findDeliveryAgent(agentId);
        return DeliveryAgentDetailResponseDto.from(deliveryAgent);
    }

    private DeliveryAgent findDeliveryAgent(UUID agentId) {
        return deliveryAgentRepository.findByIdAndDeletedAtIsNull(agentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_PERSON_NOT_FOUND));
    }
}
