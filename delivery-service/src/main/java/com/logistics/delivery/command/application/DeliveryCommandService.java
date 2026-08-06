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
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
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

    private static final Set<UserRole> DELIVERY_DELETE_ROLES = EnumSet.of(UserRole.MASTER, UserRole.HUB_MANAGER);

    @Transactional
    public CreateDeliveryResponseDto create(UserRole userRole, CreateDeliveryCommand command) {
        if (userRole != UserRole.MASTER) {
            throw new BusinessException(ErrorCode.DELIVERY_FORBIDDEN);
        }
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

        // TODO: Hub 연동(거리/소요시간 조회, 허브 간 다구간 분할) 붙기 전까지의 임시 구현.
        DeliveryRouteRecord routeRecord = DeliveryRouteRecord.builder()
            .deliveryId(saved.getId())
            .sequence(1)
            .departureHubId(command.departureHubId())
            .arrivalHubId(command.destinationHubId())
            .estimatedDistance(null)
            .estimatedDuration(null)
            .build();

        // 허브 배송 담당자는 허브 구분 없이 전체 10명 풀에서 순번대로 배정
        DeliveryAgent hubAgent = deliveryAgentAssignmentService.assignNext(AgentType.HUB_DELIVERY, null);
        routeRecord.assignAgent(hubAgent.getId());
        List<DeliveryRouteRecord> savedRouteRecords = deliveryRouteRecordRepository.saveAll(List.of(routeRecord));

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
        if (!DELIVERY_DELETE_ROLES.contains(userRole)) {
            throw new BusinessException(ErrorCode.DELIVERY_FORBIDDEN);
        }
        Delivery delivery = findDelivery(deliveryId);
        if (userRole == UserRole.HUB_MANAGER && !isWithinHub(requesterHubId, delivery)) {
            throw new BusinessException(ErrorCode.DELIVERY_FORBIDDEN);
        }
        delivery.softDelete(requesterId);

        deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId)
            .forEach(routeRecord -> routeRecord.softDelete(requesterId));

        companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(deliveryId)
            .ifPresent(companyRouteRecord -> companyRouteRecord.softDelete(requesterId));
    }

    private void validateDeliveryAccess(UserRole userRole, UUID requesterId, UUID requesterHubId, Delivery delivery,
                                         List<DeliveryRouteRecord> routeRecords) {
        if (userRole == UserRole.COMPANY_MANAGER) {
            throw new BusinessException(ErrorCode.DELIVERY_FORBIDDEN);
        }
        if (userRole == UserRole.DELIVERY_MANAGER && !isAssignedAgent(requesterId, delivery, routeRecords)) {
            throw new BusinessException(ErrorCode.DELIVERY_FORBIDDEN);
        }
        if (userRole == UserRole.HUB_MANAGER && !isWithinHub(requesterHubId, delivery)) {
            throw new BusinessException(ErrorCode.DELIVERY_FORBIDDEN);
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
