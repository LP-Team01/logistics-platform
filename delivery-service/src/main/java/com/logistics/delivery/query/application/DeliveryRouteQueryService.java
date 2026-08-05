package com.logistics.delivery.query.application;

import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.query.dto.response.DeliveryRouteDetailResponseDto;
import com.logistics.delivery.query.dto.response.DeliveryRouteResponseDto;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryRouteQueryService {
    private final DeliveryRouteRecordRepository deliveryRouteRecordRepository;

    public DeliveryRouteResponseDto getRouteRecords(UUID deliveryId) {
        List<DeliveryRouteRecord> routeRecords = deliveryRouteRecordRepository
            .findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId);
        if (routeRecords.isEmpty()) {
            throw new BusinessException(ErrorCode.DELIVERY_ROUTE_RECORD_NOT_FOUND);
        }
        return DeliveryRouteResponseDto.from(deliveryId, routeRecords);
    }


    public DeliveryRouteDetailResponseDto getRouteRecord(UUID deliveryId, Integer sequence) {
        DeliveryRouteRecord deliveryRouteRecord = deliveryRouteRecordRepository
            .findByDeliveryIdAndSequenceAndDeletedAtIsNull(deliveryId, sequence)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_ROUTE_RECORD_NOT_FOUND));

        return DeliveryRouteDetailResponseDto.from(deliveryRouteRecord);
    }
}
