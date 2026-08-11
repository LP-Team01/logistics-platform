package com.logistics.delivery.command.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.logistics.delivery.command.dto.command.UpdateDeliveryRouteRecordCommand;
import com.logistics.delivery.command.dto.response.UpdateDeliveryRouteRecordResponseDto;
import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.RouteRecordStatus;
import com.logistics.delivery.domain.repository.DeliveryRepository;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryRouteRecordCommandServiceTest {

    @Mock DeliveryRouteRecordRepository routeRecordRepository;
    @Mock DeliveryRepository deliveryRepository;
    @InjectMocks DeliveryRouteRecordCommandService deliveryRouteRecordCommandService;

    private static final UUID DEPARTURE_HUB = UUID.randomUUID();
    private static final UUID ARRIVAL_HUB = UUID.randomUUID();

    private DeliveryRouteRecord newRouteRecord(UUID deliveryId, UUID agentId) {
        return DeliveryRouteRecord.builder()
            .deliveryId(deliveryId)
            .sequence(1)
            .departureHubId(DEPARTURE_HUB)
            .arrivalHubId(ARRIVAL_HUB)
            .estimatedDistance(10)
            .estimatedDuration(10)
            .agentId(agentId)
            .build();
    }

    private void stubDeliveryExists(UUID deliveryId) {
        Delivery delivery = Delivery.builder()
            .orderId(UUID.randomUUID())
            .orderItemId(UUID.randomUUID())
            .departureHubId(DEPARTURE_HUB)
            .destinationHubId(ARRIVAL_HUB)
            .deliveryAddress("addr")
            .receiver("receiver")
            .build();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));
    }

    @Test
    @DisplayName("업체 담당자는 허브 경로 기록 상태를 변경할 권한이 없다")
    void blocksCompanyManagerRole() {
        UUID deliveryId = UUID.randomUUID();
        stubDeliveryExists(deliveryId);
        when(routeRecordRepository.findByDeliveryIdAndSequenceAndDeletedAtIsNull(deliveryId, 1))
            .thenReturn(Optional.of(newRouteRecord(deliveryId, UUID.randomUUID())));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryRouteRecordCommandService.updateStatus(deliveryId, 1,
                UpdateDeliveryRouteRecordCommand.builder().status(RouteRecordStatus.MOVING).build(),
                UserRole.COMPANY_MANAGER, UUID.randomUUID(), null));

        assertEquals(ErrorCode.DELIVERY_ROUTE_RECORD_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("배송 담당자는 본인이 배정되지 않은 구간은 수정할 수 없다")
    void blocksDeliveryManagerNotOwnAgent() {
        UUID deliveryId = UUID.randomUUID();
        stubDeliveryExists(deliveryId);
        when(routeRecordRepository.findByDeliveryIdAndSequenceAndDeletedAtIsNull(deliveryId, 1))
            .thenReturn(Optional.of(newRouteRecord(deliveryId, UUID.randomUUID())));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryRouteRecordCommandService.updateStatus(deliveryId, 1,
                UpdateDeliveryRouteRecordCommand.builder().status(RouteRecordStatus.MOVING).build(),
                UserRole.DELIVERY_MANAGER, UUID.randomUUID(), null));

        assertEquals(ErrorCode.DELIVERY_ROUTE_RECORD_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허브 관리자는 담당 허브(출발/도착)와 무관한 구간은 수정할 수 없다")
    void blocksHubManagerOutsideHub() {
        UUID deliveryId = UUID.randomUUID();
        stubDeliveryExists(deliveryId);
        when(routeRecordRepository.findByDeliveryIdAndSequenceAndDeletedAtIsNull(deliveryId, 1))
            .thenReturn(Optional.of(newRouteRecord(deliveryId, UUID.randomUUID())));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryRouteRecordCommandService.updateStatus(deliveryId, 1,
                UpdateDeliveryRouteRecordCommand.builder().status(RouteRecordStatus.MOVING).build(),
                UserRole.HUB_MANAGER, UUID.randomUUID(), UUID.randomUUID()));

        assertEquals(ErrorCode.DELIVERY_ROUTE_RECORD_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허브 도착(ARRIVED) 처리 시 실제 거리·소요시간이 없으면 거부된다")
    void requiresActualInfoOnArrival() {
        UUID deliveryId = UUID.randomUUID();
        stubDeliveryExists(deliveryId);
        DeliveryRouteRecord record = newRouteRecord(deliveryId, UUID.randomUUID());
        record.update(RouteRecordStatus.MOVING, null, null);
        when(routeRecordRepository.findByDeliveryIdAndSequenceAndDeletedAtIsNull(deliveryId, 1))
            .thenReturn(Optional.of(record));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryRouteRecordCommandService.updateStatus(deliveryId, 1,
                UpdateDeliveryRouteRecordCommand.builder().status(RouteRecordStatus.ARRIVED).build(),
                UserRole.MASTER, UUID.randomUUID(), null));

        assertEquals(ErrorCode.DELIVERY_ROUTE_RECORD_ACTUAL_INFO_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("유효한 상태전이 + 필요한 정보가 모두 있으면 정상적으로 수정된다")
    void updatesStatusSuccessfully() {
        UUID deliveryId = UUID.randomUUID();
        stubDeliveryExists(deliveryId);
        when(routeRecordRepository.findByDeliveryIdAndSequenceAndDeletedAtIsNull(deliveryId, 1))
            .thenReturn(Optional.of(newRouteRecord(deliveryId, UUID.randomUUID())));

        UpdateDeliveryRouteRecordResponseDto result = deliveryRouteRecordCommandService.updateStatus(deliveryId, 1,
            UpdateDeliveryRouteRecordCommand.builder().status(RouteRecordStatus.MOVING).build(),
            UserRole.MASTER, UUID.randomUUID(), null);

        assertEquals(RouteRecordStatus.MOVING, result.status());
    }
}