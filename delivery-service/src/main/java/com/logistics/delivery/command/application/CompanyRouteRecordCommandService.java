package com.logistics.delivery.command.application;

import com.logistics.delivery.command.dto.command.UpdateCompanyRouteRecordCommand;
import com.logistics.delivery.command.dto.response.UpdateCompanyRouteRecordResponseDto;
import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryRepository;
import com.logistics.delivery.global.common.DeliveryAccessGuard;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyRouteRecordCommandService {
    private final CompanyDeliveryRouteRecordRepository companyRouteRecordRepository;
    private final DeliveryRepository deliveryRepository;

    private static final Set<UserRole> COMPANY_ROUTE_RECORD_UPDATE_ROLES =
        EnumSet.of(UserRole.MASTER, UserRole.DELIVERY_MANAGER);

    @Transactional
    public UpdateCompanyRouteRecordResponseDto updateStatus(UUID deliveryId, UUID recordId,
                                                            UpdateCompanyRouteRecordCommand command,
                                                            UserRole userRole, UUID requesterId) {
        validateDeliveryExists(deliveryId);
        CompanyDeliveryRouteRecord routeRecord = findRouteRecord(deliveryId, recordId);
        validateAccess(userRole, requesterId, routeRecord);
        routeRecord.update(command.status(), command.actualDistance(), command.actualDuration());
        return UpdateCompanyRouteRecordResponseDto.from(routeRecord);
    }

    private void validateAccess(UserRole userRole, UUID requesterId, CompanyDeliveryRouteRecord routeRecord) {
        DeliveryAccessGuard.requireRole(userRole, COMPANY_ROUTE_RECORD_UPDATE_ROLES,
            ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN);
        if (userRole == UserRole.DELIVERY_MANAGER) {
            DeliveryAccessGuard.requireOwnAgent(requesterId, routeRecord.getAgentId(),
                ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN);
        }
    }

    private void validateDeliveryExists(UUID deliveryId) {
        deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
    }

    private CompanyDeliveryRouteRecord findRouteRecord(UUID deliveryId, UUID recordId) {
        return companyRouteRecordRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(recordId, deliveryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_ROUTE_RECORD_NOT_FOUND));
    }
}
