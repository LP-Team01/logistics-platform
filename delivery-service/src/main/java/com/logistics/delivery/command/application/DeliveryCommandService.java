package com.logistics.delivery.command.application;

import com.logistics.delivery.command.dto.command.CreateDeliveryCommand;
import com.logistics.delivery.command.dto.command.UpdateDeliveryCommand;
import com.logistics.delivery.command.dto.response.BatchCreateDeliveryResponseDto;
import com.logistics.delivery.command.dto.response.CreateDeliveryResponseDto;
import com.logistics.delivery.command.dto.response.UpdateDeliveryResponseDto;
import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.DeliveryStatus;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryOrderCoordinationRepository;
import com.logistics.delivery.domain.repository.DeliveryRepository;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
import com.logistics.delivery.global.common.DeliveryAccessGuard;
import com.logistics.delivery.global.common.FeignExceptionTranslator;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.config.HubInternalServiceProperties;
import com.logistics.delivery.global.config.InternalServiceProperties;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.infrastructure.client.HubServiceClient;
import com.logistics.delivery.infrastructure.client.dto.HubServiceRouteSegmentDto;
import com.logistics.delivery.infrastructure.kafka.DeliveryAiNotificationEvent;
import com.logistics.delivery.infrastructure.kafka.DeliveryAiNotificationEventFactory;
import com.logistics.delivery.infrastructure.kafka.DeliveryAiNotificationOutboxService;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryCommandService {
    private final DeliveryRepository deliveryRepository;
    private final DeliveryOrderCoordinationRepository deliveryOrderCoordinationRepository;
    private final DeliveryRouteRecordRepository deliveryRouteRecordRepository;
    private final CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;
    private final DeliveryAgentAssignmentService deliveryAgentAssignmentService;
    private final HubServiceClient hubServiceClient;
    private final CompanyRouteEstimator companyRouteEstimator;
    private final CompanyRouteSequencingService companyRouteSequencingService;
    private final InternalServiceProperties internalServiceProperties;
    private final HubInternalServiceProperties hubInternalServiceProperties;
    private final DeliveryAiNotificationEventFactory deliveryAiNotificationEventFactory;
    private final DeliveryAiNotificationOutboxService deliveryAiNotificationOutboxService;

    private static final Set<UserRole> DELIVERY_UPDATE_ROLES =
        EnumSet.of(UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER);
    private static final Set<UserRole> DELIVERY_DELETE_ROLES = EnumSet.of(UserRole.MASTER, UserRole.HUB_MANAGER);
    private static final int MAX_BATCH_SIZE = 20;

    @Transactional
    public CreateDeliveryResponseDto create(CreateDeliveryCommand command) {
        return createOne(command);
    }

    // Order 서비스가 주문 1건의 상품별 배송을 반복 호출 없이 한 번에 생성하기 위한 API.
    @Transactional
    public List<BatchCreateDeliveryResponseDto> createBatch(List<CreateDeliveryCommand> commands) {
        validateBatchSize(commands);
        return commands.stream()
            .map(this::createOne)
            .map(BatchCreateDeliveryResponseDto::from)
            .toList();
    }

    private CreateDeliveryResponseDto createOne(CreateDeliveryCommand command) {
        validateOrderNotCancelled(command.orderId());
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
        CompanyDeliveryRouteRecord companyRouteRecord = buildCompanyRouteRecord(saved.getId(), command);
        companyDeliveryRouteRecordRepository.save(companyRouteRecord);

        publishDeliveryCreatedEvent(saved, command, savedRouteRecords, companyRouteRecord);

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
        cancelDelivery(delivery);
    }

    // Order 서비스의 주문 생성 실패 보상 처리 전용
    @Transactional
    public void cancelByOrderId(UUID orderId) {
        deliveryOrderCoordinationRepository.ensureExists(orderId);
        deliveryOrderCoordinationRepository.findByOrderIdForUpdate(orderId)
            .orElseThrow(() -> new IllegalStateException("배송 주문 조정 정보가 없습니다."))
            .cancel();

        List<Delivery> deliveries = deliveryRepository.findByOrderIdAndDeletedAtIsNull(orderId);
        deliveries.forEach(this::cancelDelivery);
    }

    private void cancelDelivery(Delivery delivery) {
        delivery.softDelete(null);
        deliveryRouteRecordRepository
            .findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(delivery.getId())
            .forEach(routeRecord -> routeRecord.softDelete(null));
        companyDeliveryRouteRecordRepository
            .findByDeliveryIdAndDeletedAtIsNull(delivery.getId())
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

    private CompanyDeliveryRouteRecord buildCompanyRouteRecord(UUID deliveryId, CreateDeliveryCommand command) {
        CompanyRouteEstimator.CompanyRouteEstimate estimate =
            companyRouteEstimator.estimate(command.destinationHubId(), command.deliveryAddress());

        return CompanyDeliveryRouteRecord.builder()
            .deliveryId(deliveryId)
            .departureHubId(command.destinationHubId())
            .receiverCompanyId(command.receiverCompanyId())
            .latitude(estimate.latitude())
            .longitude(estimate.longitude())
            .estimatedDistance(estimate.distanceKm())
            .estimatedDuration(estimate.durationMin())
            .build();
    }

    // DELIVERY_CREATED 이벤트는 AI 최종 발송 시한 계산/Slack 발송을 위한 부가 데이터라, 조립에 실패해도
    // 배송 생성 자체는 막지 않는다 (DeliveryAiNotificationEventFactory가 실패 시 Optional.empty() 반환).
    private void publishDeliveryCreatedEvent(Delivery delivery, CreateDeliveryCommand command,
                                              List<DeliveryRouteRecord> routeRecords,
                                              CompanyDeliveryRouteRecord companyRouteRecord) {
        Optional<DeliveryAiNotificationEvent> event = deliveryAiNotificationEventFactory
            .create(delivery, command, routeRecords, companyRouteRecord);
        event.ifPresent(deliveryAiNotificationOutboxService::save);
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
        companyRouteSequencingService.resequence(companyAgent.getId(), delivery.getDestinationHubId());
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

    /** 취소가 먼저 접수된 주문은 이후 도착한 배송 생성 요청을 거부합니다. */
    private void validateOrderNotCancelled(UUID orderId) {
        deliveryOrderCoordinationRepository.ensureExists(orderId);
        boolean cancelled = deliveryOrderCoordinationRepository
            .findByOrderIdForUpdate(orderId)
            .orElseThrow(() -> new IllegalStateException("배송 주문 조정 정보가 없습니다."))
            .isCancelled();

        if (cancelled) {
            throw new BusinessException(ErrorCode.DELIVERY_ORDER_CANCELLED);
        }
    }

    private void validateBatchSize(List<CreateDeliveryCommand> commands) {
        if (commands.isEmpty()) {
            throw new BusinessException(ErrorCode.DELIVERY_BATCH_REQUEST_EMPTY);
        }
        if (commands.size() > MAX_BATCH_SIZE) {
            throw new BusinessException(ErrorCode.DELIVERY_BATCH_REQUEST_SIZE_EXCEEDED);
        }
    }
}
