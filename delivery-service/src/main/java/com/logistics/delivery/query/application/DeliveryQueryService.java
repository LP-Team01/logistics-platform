package com.logistics.delivery.query.application;

import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.repository.DeliveryRepository;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliverySpecification;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.query.dto.reponse.DeliveryDetailResponseDto;
import com.logistics.delivery.query.dto.reponse.DeliveryResponseDto;
import com.logistics.delivery.query.dto.request.DeliverySearchRequestDto;
import java.util.List;
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
public class DeliveryQueryService {
    private final DeliveryRepository deliveryRepository;
    private final DeliveryRouteRecordRepository deliveryRouteRecordRepository;

    public DeliveryDetailResponseDto getDelivery(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        List<DeliveryRouteRecord> routeRecords = deliveryRouteRecordRepository
            .findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId);

        return DeliveryDetailResponseDto.from(delivery, routeRecords);
    }

    public DeliveryResponseDto searchDelivery(DeliverySearchRequestDto request, Pageable pageable) {
        // TODO: 게이트웨이의 X-User-Id/X-User-Role 전달이 구현되면, COMPANY 역할은 요청자 X-User-Id로
        Specification<Delivery> spec = DeliverySpecification.withSearchCondition(
            request.status(), request.orderId(), request.orderItemId(), request.companyAgentId());
        Page<Delivery> page = deliveryRepository.findAll(spec, pageable);
        return DeliveryResponseDto.from(page);
    }
}
