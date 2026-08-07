package com.logistics.delivery.command.application;

import com.logistics.delivery.command.dto.command.CreateDeliveryCommand;
import com.logistics.delivery.command.dto.command.UpdateDeliveryCommand;
import com.logistics.delivery.command.dto.response.CreateDeliveryResponseDto;
import com.logistics.delivery.command.dto.response.UpdateDeliveryResponseDto;
import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.DeliveryStatus;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryRepository;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
import com.logistics.delivery.domain.service.DeliveryAgentAssignmentService;
import com.logistics.delivery.global.common.DeliveryAccessGuard;
import com.logistics.delivery.global.common.FeignExceptionTranslator;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.config.HubInternalServiceProperties;
import com.logistics.delivery.global.config.InternalServiceProperties;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.infrastructure.client.HubServiceClient;
import com.logistics.delivery.infrastructure.client.dto.HubServiceRouteSegmentDto;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryCommandService {
    private final DeliveryRepository deliveryRepository;
    private final DeliveryRouteRecordRepository deliveryRouteRecordRepository;
    private final CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;
    private final DeliveryAgentAssignmentService deliveryAgentAssignmentService;
    private final HubServiceClient hubServiceClient;
    private final InternalServiceProperties internalServiceProperties;
    private final HubInternalServiceProperties hubInternalServiceProperties;

    private static final Set<UserRole> DELIVERY_UPDATE_ROLES =
        EnumSet.of(UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER);
    private static final Set<UserRole> DELIVERY_DELETE_ROLES = EnumSet.of(UserRole.MASTER, UserRole.HUB_MANAGER);

    @Transactional
    public CreateDeliveryResponseDto create(CreateDeliveryCommand command) {
        validateOrderItem(command.orderItemId());
        Delivery delivery = Delivery.builder()
            .orderId(command.orderId())
            .orderItemId(command.orderItemId())
            .departureHubId(command.departureHubId())
            .destinationHubId(command.destinationHubId())
            .deliveryAddress(command.deliveryAddress())
            .receiver(command.receiver())
            .receiverSlackId(command.receiverSlackId())
            .build();

        Delivery saved = deliveryRepository.save(delivery);

        List<HubServiceRouteSegmentDto> path = findRoutePath(command.departureHubId(), command.destinationHubId());
        // 구간마다 즉시 저장 - assignNext()가 직전 구간의 배정 결과를 이어서 조회하므로 배치 저장하면 순번이 꼬임
        List<DeliveryRouteRecord> savedRouteRecords = new ArrayList<>();
        for (HubServiceRouteSegmentDto segment : path) {
            savedRouteRecords.add(buildAndSaveRouteRecord(saved.getId(), segment));
        }

        // 목적지 허브 → 수령 업체 구간. 업체배송담당자는 DESTINATION_ARRIVED 도달 시점에 배정(agentId는 null로 시작).
        CompanyDeliveryRouteRecord companyRouteRecord = CompanyDeliveryRouteRecord.builder()
            .deliveryId(saved.getId())
            .departureHubId(command.destinationHubId())
            .receiverCompanyId(command.receiverCompanyId())
            .build();
        companyDeliveryRouteRecordRepository.save(companyRouteRecord);

        return CreateDeliveryResponseDto.from(saved, savedRouteRecords);
    }

    @Transactional
    public UpdateDeliveryResponseDto update(UserRole userRole, UUID requesterId, UUID requesterHubId,
                                             UUID deliveryId, UpdateDeliveryCommand command) {
        Delivery delivery = findDelivery(deliveryId);
        List<DeliveryRouteRecord> routeRecords = deliveryRouteRecordRepository
            .findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId);
        validateDeliveryAccess(userRole, requesterId, requesterHubId, delivery, routeRecords);

        delivery.update(command.status());
        if (command.status() == DeliveryStatus.DESTINATION_ARRIVED) {
            assignCompanyAgent(delivery);
        }
        return UpdateDeliveryResponseDto.from(delivery);
    }

    @Transactional
    public void delete(UserRole userRole, UUID requesterId, UUID requesterHubId, UUID deliveryId) {
        DeliveryAccessGuard.requireRole(userRole, DELIVERY_DELETE_ROLES, ErrorCode.DELIVERY_FORBIDDEN);
        Delivery delivery = findDelivery(deliveryId);
        if (userRole == UserRole.HUB_MANAGER) {
            DeliveryAccessGuard.requireWithinHub(requesterHubId, ErrorCode.DELIVERY_FORBIDDEN,
                delivery.getDepartureHubId(), delivery.getDestinationHubId());
        }
        delivery.softDelete(requesterId);

        deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId)
            .forEach(routeRecord -> routeRecord.softDelete(requesterId));

        companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(deliveryId)
            .ifPresent(companyRouteRecord -> companyRouteRecord.softDelete(requesterId));
    }

    // Order 서비스의 주문 취소 콜백 전용. 호출자가 사람이 아니라 deletedBy(감사 정보)는 null
    @Transactional
    public void deleteByOrderItem(UUID orderItemId) {
        Delivery delivery = deliveryRepository.findByOrderItemIdAndDeletedAtIsNull(orderItemId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
        delivery.softDelete(null);

        deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(delivery.getId())
            .forEach(routeRecord -> routeRecord.softDelete(null));

        companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(delivery.getId())
            .ifPresent(companyRouteRecord -> companyRouteRecord.softDelete(null));
    }

    private void validateDeliveryAccess(UserRole userRole, UUID requesterId, UUID requesterHubId, Delivery delivery,
                                         List<DeliveryRouteRecord> routeRecords) {
        DeliveryAccessGuard.requireRole(userRole, DELIVERY_UPDATE_ROLES, ErrorCode.DELIVERY_FORBIDDEN);
        if (userRole == UserRole.DELIVERY_MANAGER) {
            List<UUID> routeAgentIds = routeRecords.stream().map(DeliveryRouteRecord::getAgentId).toList();
            DeliveryAccessGuard.requireAssignedAgent(requesterId, delivery.getCompanyAgentId(), routeAgentIds,
                ErrorCode.DELIVERY_FORBIDDEN);
        }
        if (userRole == UserRole.HUB_MANAGER) {
            DeliveryAccessGuard.requireWithinHub(requesterHubId, ErrorCode.DELIVERY_FORBIDDEN,
                delivery.getDepartureHubId(), delivery.getDestinationHubId());
        }
    }

    private List<HubServiceRouteSegmentDto> findRoutePath(UUID departureHubId, UUID destinationHubId) {
        return FeignExceptionTranslator.call(
            () -> hubServiceClient.getRoutePath(
                internalServiceProperties.name(), hubInternalServiceProperties.key(),
                departureHubId, destinationHubId
            ).path(),
            ErrorCode.DELIVERY_HUB_NOT_FOUND,
            ErrorCode.INVALID_DELIVERY_ROUTE
        );
    }

    private DeliveryRouteRecord buildAndSaveRouteRecord(UUID deliveryId, HubServiceRouteSegmentDto segment) {
        DeliveryRouteRecord routeRecord = DeliveryRouteRecord.builder()
            .deliveryId(deliveryId)
            .sequence(segment.sequence() + 1)
            .departureHubId(segment.departureHubId())
            .arrivalHubId(segment.arrivalHubId())
            .estimatedDistance((int) Math.round(segment.distance()))
            .estimatedDuration(segment.duration())
            .build();

        // 허브 배송 담당자는 허브 구분 없이 전체 10명 풀에서 구간마다 순번대로 배정
        DeliveryAgent hubAgent = deliveryAgentAssignmentService.assignNext(AgentType.HUB_DELIVERY, null);
        routeRecord.assignAgent(hubAgent.getId());
        return deliveryRouteRecordRepository.save(routeRecord);
    }

    private void assignCompanyAgent(Delivery delivery) {
        CompanyDeliveryRouteRecord companyRouteRecord = companyDeliveryRouteRecordRepository
            .findByDeliveryIdAndDeletedAtIsNull(delivery.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_ROUTE_RECORD_NOT_FOUND));
        DeliveryAgent companyAgent = deliveryAgentAssignmentService
            .assignNext(AgentType.COMPANY_DELIVERY, delivery.getDestinationHubId());
        companyRouteRecord.assignAgent(companyAgent.getId());
        delivery.assignCompanyAgent(companyAgent.getId());
    }

    private Delivery findDelivery(UUID deliveryId) {
        return deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
    }

    private void validateOrderItem(UUID orderItemId) {
        boolean exists = deliveryRepository.existsByOrderItemIdAndDeletedAtIsNull(orderItemId);
        if (exists) {
            throw new BusinessException(ErrorCode.DELIVERY_ORDER_ALREADY_EXISTS);
        }
    }
}
