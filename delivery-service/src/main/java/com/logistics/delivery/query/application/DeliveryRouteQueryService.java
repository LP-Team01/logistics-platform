package com.logistics.delivery.query.application;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
import com.logistics.delivery.global.common.DeliveryAccessGuard;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.query.dto.response.DeliveryRouteDetailResponseDto;
import com.logistics.delivery.query.dto.response.DeliveryRouteResponseDto;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryRouteQueryService {
    private final DeliveryRouteRecordRepository deliveryRouteRecordRepository;
    private final CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;

    public DeliveryRouteResponseDto getRouteRecords(UserRole userRole, UUID requesterId, UUID requesterHubId,
                                                      UUID requesterCompanyId, UUID deliveryId) {
        List<DeliveryRouteRecord> routeRecords = deliveryRouteRecordRepository
            .findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId);
        if (routeRecords.isEmpty()) {
            throw new BusinessException(ErrorCode.DELIVERY_ROUTE_RECORD_NOT_FOUND);
        }
        validateRouteAccess(userRole, requesterId, requesterHubId, requesterCompanyId, deliveryId, routeRecords);
        return DeliveryRouteResponseDto.from(deliveryId, routeRecords);
    }


    public DeliveryRouteDetailResponseDto getRouteRecord(UserRole userRole, UUID requesterId, UUID requesterHubId,
                                                          UUID requesterCompanyId, UUID deliveryId, Integer sequence) {
        DeliveryRouteRecord deliveryRouteRecord = deliveryRouteRecordRepository
            .findByDeliveryIdAndSequenceAndDeletedAtIsNull(deliveryId, sequence)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_ROUTE_RECORD_NOT_FOUND));

        validateRouteAccess(userRole, requesterId, requesterHubId, requesterCompanyId, deliveryId,
            List.of(deliveryRouteRecord));
        return DeliveryRouteDetailResponseDto.from(deliveryRouteRecord);
    }

    private void validateRouteAccess(UserRole userRole, UUID requesterId, UUID requesterHubId,
                                      UUID requesterCompanyId, UUID deliveryId,
                                      List<DeliveryRouteRecord> routeRecords) {
        if (userRole == UserRole.DELIVERY_MANAGER) {
            List<UUID> routeAgentIds = routeRecords.stream().map(DeliveryRouteRecord::getAgentId).toList();
            DeliveryAccessGuard.requireAssignedAgent(requesterId, null, routeAgentIds,
                ErrorCode.DELIVERY_ROUTE_RECORD_FORBIDDEN);
        }
        if (userRole == UserRole.HUB_MANAGER) {
            UUID[] hubIds = routeRecords.stream()
                .flatMap(record -> Stream.of(record.getDepartureHubId(), record.getArrivalHubId()))
                .toArray(UUID[]::new);
            DeliveryAccessGuard.requireWithinHub(requesterHubId, ErrorCode.DELIVERY_ROUTE_RECORD_FORBIDDEN, hubIds);
        }
        if (userRole == UserRole.COMPANY_MANAGER) {
            DeliveryAccessGuard.requireOwnCompany(requesterCompanyId, ownCompanyId(deliveryId),
                ErrorCode.DELIVERY_ROUTE_RECORD_FORBIDDEN);
        }
    }

    private UUID ownCompanyId(UUID deliveryId) {
        return companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(deliveryId)
            .map(CompanyDeliveryRouteRecord::getReceiverCompanyId)
            .orElse(null);
    }
}
