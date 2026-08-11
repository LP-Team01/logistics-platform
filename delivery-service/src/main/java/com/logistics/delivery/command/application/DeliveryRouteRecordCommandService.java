package com.logistics.delivery.command.application;

import com.logistics.delivery.command.dto.command.UpdateDeliveryRouteRecordCommand;
import com.logistics.delivery.command.dto.response.UpdateDeliveryRouteRecordResponseDto;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.repository.DeliveryRepository;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
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
public class DeliveryRouteRecordCommandService {
    private final DeliveryRouteRecordRepository routeRecordRepository;
    private final DeliveryRepository deliveryRepository;

    private static final Set<UserRole> ROUTE_RECORD_UPDATE_ROLES =
        EnumSet.of(UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER);

    @Transactional
    public UpdateDeliveryRouteRecordResponseDto updateStatus(UUID deliveryId, Integer sequence,
                                                             UpdateDeliveryRouteRecordCommand command,
                                                             UserRole userRole, UUID requesterId,
                                                             UUID requesterHubId) {
        validateDeliveryExists(deliveryId);
        DeliveryRouteRecord routeRecord = findRouteRecord(deliveryId, sequence);
        validateAccess(userRole, requesterId, requesterHubId, routeRecord);
        routeRecord.update(command.status(), command.actualDistance(), command.actualDuration());
        return UpdateDeliveryRouteRecordResponseDto.from(routeRecord);
    }

    private void validateAccess(UserRole userRole, UUID requesterId, UUID requesterHubId,
                                 DeliveryRouteRecord routeRecord) {
        DeliveryAccessGuard.requireRole(userRole, ROUTE_RECORD_UPDATE_ROLES, ErrorCode.DELIVERY_ROUTE_RECORD_FORBIDDEN);
        if (userRole == UserRole.DELIVERY_MANAGER) {
            DeliveryAccessGuard.requireOwnAgent(requesterId, routeRecord.getAgentId(),
                ErrorCode.DELIVERY_ROUTE_RECORD_FORBIDDEN);
        }
        if (userRole == UserRole.HUB_MANAGER) {
            DeliveryAccessGuard.requireWithinHub(requesterHubId, ErrorCode.DELIVERY_ROUTE_RECORD_FORBIDDEN,
                routeRecord.getDepartureHubId(), routeRecord.getArrivalHubId());
        }
    }

    private void validateDeliveryExists(UUID deliveryId) {
        deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
    }

    private DeliveryRouteRecord findRouteRecord(UUID deliveryId, Integer sequence) {
        return routeRecordRepository.findByDeliveryIdAndSequenceAndDeletedAtIsNull(deliveryId, sequence)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_ROUTE_RECORD_NOT_FOUND));
    }
}
