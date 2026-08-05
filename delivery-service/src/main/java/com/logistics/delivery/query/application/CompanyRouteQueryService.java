package com.logistics.delivery.query.application;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.query.dto.response.CompanyRouteResponseDto;
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
