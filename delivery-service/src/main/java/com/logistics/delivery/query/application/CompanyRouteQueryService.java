package com.logistics.delivery.query.application;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.query.dto.reponse.CompanyRouteResponseDto;
import com.logistics.delivery.query.dto.reponse.DeliveryRouteDetailResponseDto;
import com.logistics.delivery.query.dto.reponse.DeliveryRouteResponseDto;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyRouteQueryService {
    private final CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;

    public CompanyRouteResponseDto getCompanyRouteRecords(UUID deliveryId) {
        CompanyDeliveryRouteRecord companyDeliveryRouteRecord = companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(
                deliveryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_ROUTE_RECORD_NOT_FOUND));

        return CompanyRouteResponseDto.from(companyDeliveryRouteRecord);
    }
}
