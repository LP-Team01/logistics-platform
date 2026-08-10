package com.logistics.delivery.command.application;

import com.logistics.delivery.command.dto.command.UpdateCompanyRouteRecordCommand;
import com.logistics.delivery.command.dto.command.UpdateCompanyRouteRecordPlanCommand;
import com.logistics.delivery.command.dto.response.UpdateCompanyRouteRecordPlanResponseDto;
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
    // 계획 데이터(좌표/예상거리/예상시간/방문순서) 수동 보정은 자동 계산 실패를 바로잡는 운영 조직 상위 권한으로 제한
    private static final Set<UserRole> COMPANY_ROUTE_RECORD_PLAN_UPDATE_ROLES =
        EnumSet.of(UserRole.MASTER, UserRole.HUB_MANAGER);

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

    // Geocoding/Directions/방문순서 자동 계산이 3회 재시도 후에도 실패했을 때의 수동 보정
    @Transactional
    public UpdateCompanyRouteRecordPlanResponseDto updateRoutePlan(UUID deliveryId, UUID recordId,
                                                                    UpdateCompanyRouteRecordPlanCommand command,
                                                                    UserRole userRole, UUID requesterHubId) {
        validateDeliveryExists(deliveryId);
        CompanyDeliveryRouteRecord routeRecord = findRouteRecord(deliveryId, recordId);
        validatePlanAccess(userRole, requesterHubId, routeRecord);
        routeRecord.updateRoutePlan(command.latitude(), command.longitude(), command.estimatedDistance(),
            command.estimatedDuration(), command.deliverySequence());
        return UpdateCompanyRouteRecordPlanResponseDto.from(routeRecord);
    }

    private void validateAccess(UserRole userRole, UUID requesterId, CompanyDeliveryRouteRecord routeRecord) {
        DeliveryAccessGuard.requireRole(userRole, COMPANY_ROUTE_RECORD_UPDATE_ROLES,
            ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN);
        if (userRole == UserRole.DELIVERY_MANAGER) {
            DeliveryAccessGuard.requireOwnAgent(requesterId, routeRecord.getAgentId(),
                ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN);
        }
    }

    private void validatePlanAccess(UserRole userRole, UUID requesterHubId, CompanyDeliveryRouteRecord routeRecord) {
        DeliveryAccessGuard.requireRole(userRole, COMPANY_ROUTE_RECORD_PLAN_UPDATE_ROLES,
            ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN);
        if (userRole == UserRole.HUB_MANAGER) {
            DeliveryAccessGuard.requireWithinHub(requesterHubId, ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN,
                routeRecord.getDepartureHubId());
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
