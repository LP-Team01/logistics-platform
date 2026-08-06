package com.logistics.delivery.query.application;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryRepository;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliverySpecification;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.query.dto.response.DeliveryDetailResponseDto;
import com.logistics.delivery.query.dto.response.DeliveryResponseDto;
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
    private final CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;

    public DeliveryDetailResponseDto getDelivery(UserRole userRole, UUID requesterId, UUID requesterHubId,
                                                  UUID requesterCompanyId, UUID deliveryId) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        List<DeliveryRouteRecord> routeRecords = deliveryRouteRecordRepository
            .findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId);

        validateDeliveryAccess(userRole, requesterId, requesterHubId, requesterCompanyId, delivery, routeRecords);

        return DeliveryDetailResponseDto.from(delivery, routeRecords);
    }

    public DeliveryResponseDto searchDelivery(UserRole userRole, UUID requesterId, UUID requesterHubId,
                                               UUID requesterCompanyId, DeliverySearchRequestDto request,
                                               Pageable pageable) {
        UUID assignedAgentId = userRole == UserRole.DELIVERY_MANAGER ? requesterId : null;
        UUID hubId = null;
        if (userRole == UserRole.HUB_MANAGER) {
            if (requesterHubId == null) {
                throw new BusinessException(ErrorCode.DELIVERY_QUERY_FORBIDDEN);
            }
            hubId = requesterHubId;
        }
        UUID companyId = null;
        if (userRole == UserRole.COMPANY_MANAGER) {
            if (requesterCompanyId == null) {
                throw new BusinessException(ErrorCode.DELIVERY_QUERY_FORBIDDEN);
            }
            companyId = requesterCompanyId;
        }
        Specification<Delivery> spec = DeliverySpecification.withSearchCondition(
            request.status(), request.orderId(), request.orderItemId(), request.companyAgentId(),
            assignedAgentId, hubId, companyId);
        Page<Delivery> page = deliveryRepository.findAll(spec, pageable);
        return DeliveryResponseDto.from(page);
    }

    private void validateDeliveryAccess(UserRole userRole, UUID requesterId, UUID requesterHubId,
                                         UUID requesterCompanyId, Delivery delivery,
                                         List<DeliveryRouteRecord> routeRecords) {
        if (userRole == UserRole.DELIVERY_MANAGER && !isAssignedAgent(requesterId, delivery, routeRecords)) {
            throw new BusinessException(ErrorCode.DELIVERY_QUERY_FORBIDDEN);
        }
        if (userRole == UserRole.HUB_MANAGER && !isWithinHub(requesterHubId, delivery)) {
            throw new BusinessException(ErrorCode.DELIVERY_QUERY_FORBIDDEN);
        }
        if (userRole == UserRole.COMPANY_MANAGER && !isOwnCompany(requesterCompanyId, delivery)) {
            throw new BusinessException(ErrorCode.DELIVERY_QUERY_FORBIDDEN);
        }
    }

    private boolean isAssignedAgent(UUID requesterId, Delivery delivery, List<DeliveryRouteRecord> routeRecords) {
        return requesterId.equals(delivery.getCompanyAgentId())
            || routeRecords.stream().anyMatch(record -> requesterId.equals(record.getAgentId()));
    }

    private boolean isWithinHub(UUID requesterHubId, Delivery delivery) {
        return requesterHubId != null
            && (requesterHubId.equals(delivery.getDepartureHubId())
                || requesterHubId.equals(delivery.getDestinationHubId()));
    }

    private boolean isOwnCompany(UUID requesterCompanyId, Delivery delivery) {
        if (requesterCompanyId == null) {
            return false;
        }
        return companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(delivery.getId())
            .map(CompanyDeliveryRouteRecord::getReceiverCompanyId)
            .map(requesterCompanyId::equals)
            .orElse(false);
    }
}
