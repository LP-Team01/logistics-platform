package com.logistics.delivery.query.application;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.global.common.DeliveryAccessGuard;
import com.logistics.delivery.global.common.UserRole;
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

    public CompanyRouteResponseDto getCompanyRouteRecords(UserRole userRole, UUID requesterId, UUID requesterHubId,
                                                           UUID requesterCompanyId, UUID deliveryId) {
        CompanyDeliveryRouteRecord companyDeliveryRouteRecord = companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(
                deliveryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_ROUTE_RECORD_NOT_FOUND));

        if (userRole == UserRole.DELIVERY_MANAGER) {
            DeliveryAccessGuard.requireOwnAgent(requesterId, companyDeliveryRouteRecord.getAgentId(),
                ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN);
        }
        if (userRole == UserRole.HUB_MANAGER) {
            DeliveryAccessGuard.requireWithinHub(requesterHubId, ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN,
                companyDeliveryRouteRecord.getDepartureHubId());
        }
        if (userRole == UserRole.COMPANY_MANAGER) {
            DeliveryAccessGuard.requireOwnCompany(requesterCompanyId, companyDeliveryRouteRecord.getReceiverCompanyId(),
                ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN);
        }

        return CompanyRouteResponseDto.from(companyDeliveryRouteRecord);
    }
}
