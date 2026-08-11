package com.logistics.delivery.command.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.delivery.command.dto.command.UpdateDeliveryCommand;
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
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryCommandServiceTest {

    @Mock DeliveryRepository deliveryRepository;
    @Mock DeliveryRouteRecordRepository deliveryRouteRecordRepository;
    @Mock CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;
    @Mock DeliveryAgentAssignmentService deliveryAgentAssignmentService;
    @Mock CompanyRouteSequencingService companyRouteSequencingService;
    @InjectMocks DeliveryCommandService deliveryCommandService;

    private static final UUID DEPARTURE_HUB = UUID.randomUUID();
    private static final UUID DESTINATION_HUB = UUID.randomUUID();

    private Delivery newDelivery() {
        return Delivery.builder()
            .orderId(UUID.randomUUID())
            .orderItemId(UUID.randomUUID())
            .departureHubId(DEPARTURE_HUB)
            .destinationHubId(DESTINATION_HUB)
            .deliveryAddress("서울시 강남구")
            .receiver("홍길동")
            .build();
    }

    @Test
    @DisplayName("상태전이 규칙표에 정의된 다음 상태로는 정상적으로 변경된다")
    void updatesStatusForValidTransition() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = newDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId))
            .thenReturn(List.of());

        UpdateDeliveryResponseDto result = deliveryCommandService.update(
            UserRole.MASTER, UUID.randomUUID(), null, deliveryId,
            UpdateDeliveryCommand.builder().status(DeliveryStatus.HUB_MOVING).build());

        assertEquals(DeliveryStatus.HUB_MOVING, result.status());
    }

    @Test
    @DisplayName("상태전이 규칙표를 건너뛰는(스킵) 전이는 거부된다")
    void rejectsInvalidStatusTransition() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = newDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId))
            .thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> deliveryCommandService.update(
            UserRole.MASTER, UUID.randomUUID(), null, deliveryId,
            UpdateDeliveryCommand.builder().status(DeliveryStatus.DELIVERED).build()));

        assertEquals(ErrorCode.DELIVERY_STATUS_NOT_CHANGEABLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("업체 담당자는 배송 상태 변경 권한이 아예 없다")
    void blocksCompanyManagerFromUpdating() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = newDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId))
            .thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> deliveryCommandService.update(
            UserRole.COMPANY_MANAGER, UUID.randomUUID(), null, deliveryId,
            UpdateDeliveryCommand.builder().status(DeliveryStatus.HUB_MOVING).build()));

        assertEquals(ErrorCode.DELIVERY_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("배송 담당자는 본인이 배정되지 않은 배송은 수정할 수 없다")
    void blocksDeliveryManagerNotAssignedToDelivery() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = newDelivery();
        DeliveryRouteRecord routeRecord = DeliveryRouteRecord.builder()
            .deliveryId(deliveryId)
            .sequence(1)
            .departureHubId(DEPARTURE_HUB)
            .arrivalHubId(DESTINATION_HUB)
            .estimatedDistance(10)
            .estimatedDuration(10)
            .agentId(UUID.randomUUID())
            .build();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId))
            .thenReturn(List.of(routeRecord));

        BusinessException exception = assertThrows(BusinessException.class, () -> deliveryCommandService.update(
            UserRole.DELIVERY_MANAGER, UUID.randomUUID(), null, deliveryId,
            UpdateDeliveryCommand.builder().status(DeliveryStatus.HUB_MOVING).build()));

        assertEquals(ErrorCode.DELIVERY_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허브 구간 담당자로 배정된 배송 담당자는 본인 담당 배송을 수정할 수 있다")
    void allowsDeliveryManagerAssignedViaRouteRecord() {
        UUID deliveryId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Delivery delivery = newDelivery();
        DeliveryRouteRecord routeRecord = DeliveryRouteRecord.builder()
            .deliveryId(deliveryId)
            .sequence(1)
            .departureHubId(DEPARTURE_HUB)
            .arrivalHubId(DESTINATION_HUB)
            .estimatedDistance(10)
            .estimatedDuration(10)
            .agentId(requesterId)
            .build();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId))
            .thenReturn(List.of(routeRecord));

        UpdateDeliveryResponseDto result = deliveryCommandService.update(
            UserRole.DELIVERY_MANAGER, requesterId, null, deliveryId,
            UpdateDeliveryCommand.builder().status(DeliveryStatus.HUB_MOVING).build());

        assertEquals(DeliveryStatus.HUB_MOVING, result.status());
    }

    @Test
    @DisplayName("허브 관리자는 담당 허브(출발/도착)와 무관한 배송은 수정할 수 없다")
    void blocksHubManagerOutsideHub() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = newDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId))
            .thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> deliveryCommandService.update(
            UserRole.HUB_MANAGER, UUID.randomUUID(), UUID.randomUUID(), deliveryId,
            UpdateDeliveryCommand.builder().status(DeliveryStatus.HUB_MOVING).build()));

        assertEquals(ErrorCode.DELIVERY_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("목적지 허브 도착 시점에 업체배송담당자가 배정되고 방문 순서 재계산이 트리거된다")
    void assignsCompanyAgentWhenReachingDestinationArrived() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = newDelivery();
        delivery.update(DeliveryStatus.HUB_MOVING);
        CompanyDeliveryRouteRecord companyRouteRecord = CompanyDeliveryRouteRecord.builder()
            .deliveryId(deliveryId)
            .departureHubId(DESTINATION_HUB)
            .receiverCompanyId(UUID.randomUUID())
            .build();
        DeliveryAgent companyAgent = DeliveryAgent.builder()
            .agentId(UUID.randomUUID())
            .hubId(DESTINATION_HUB)
            .agentType(AgentType.COMPANY_DELIVERY)
            .deliveryOrder(0)
            .isAvailable(true)
            .build();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId))
            .thenReturn(List.of());
        when(companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(any()))
            .thenReturn(Optional.of(companyRouteRecord));
        when(deliveryAgentAssignmentService.assignNext(AgentType.COMPANY_DELIVERY, DESTINATION_HUB))
            .thenReturn(companyAgent);

        deliveryCommandService.update(
            UserRole.MASTER, UUID.randomUUID(), null, deliveryId,
            UpdateDeliveryCommand.builder().status(DeliveryStatus.DESTINATION_ARRIVED).build());

        assertEquals(companyAgent.getId(), delivery.getCompanyAgentId());
        assertEquals(companyAgent.getId(), companyRouteRecord.getAgentId());
        verify(companyRouteSequencingService).resequence(companyAgent.getId(), DESTINATION_HUB);
    }

    @Test
    @DisplayName("배송 담당자는 배송을 삭제할 권한 자체가 없다(마스터/허브 관리자만 가능)")
    void blocksNonDeleteRoleFromDeleting() {
        BusinessException exception = assertThrows(BusinessException.class, () -> deliveryCommandService.delete(
            UserRole.DELIVERY_MANAGER, UUID.randomUUID(), null, UUID.randomUUID()));

        assertEquals(ErrorCode.DELIVERY_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허브 관리자는 담당 허브와 무관한 배송은 삭제할 수 없다")
    void blocksHubManagerOutsideHubFromDeleting() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = newDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));

        BusinessException exception = assertThrows(BusinessException.class, () -> deliveryCommandService.delete(
            UserRole.HUB_MANAGER, UUID.randomUUID(), UUID.randomUUID(), deliveryId));

        assertEquals(ErrorCode.DELIVERY_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("배송 삭제 시 하위 허브 경로 기록/업체 배송 경로 기록도 함께 논리 삭제된다")
    void softDeletesDeliveryAndCascadesToRouteRecords() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = newDelivery();
        DeliveryRouteRecord routeRecord = DeliveryRouteRecord.builder()
            .deliveryId(deliveryId)
            .sequence(1)
            .departureHubId(DEPARTURE_HUB)
            .arrivalHubId(DESTINATION_HUB)
            .estimatedDistance(10)
            .estimatedDuration(10)
            .build();
        CompanyDeliveryRouteRecord companyRouteRecord = CompanyDeliveryRouteRecord.builder()
            .deliveryId(deliveryId)
            .departureHubId(DESTINATION_HUB)
            .receiverCompanyId(UUID.randomUUID())
            .build();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId))
            .thenReturn(List.of(routeRecord));
        when(companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(deliveryId))
            .thenReturn(Optional.of(companyRouteRecord));

        deliveryCommandService.delete(UserRole.MASTER, UUID.randomUUID(), null, deliveryId);

        assertNotNull(delivery.getDeletedAt());
        assertNotNull(routeRecord.getDeletedAt());
        assertNotNull(companyRouteRecord.getDeletedAt());
    }
}