package com.logistics.delivery.command.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.logistics.delivery.command.dto.command.UpdateCompanyRouteRecordCommand;
import com.logistics.delivery.command.dto.command.UpdateCompanyRouteRecordPlanCommand;
import com.logistics.delivery.command.dto.response.UpdateCompanyRouteRecordPlanResponseDto;
import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryRepository;
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
class CompanyRouteRecordCommandServiceTest {

    @Mock CompanyDeliveryRouteRecordRepository companyRouteRecordRepository;
    @Mock DeliveryRepository deliveryRepository;
    @InjectMocks CompanyRouteRecordCommandService companyRouteRecordCommandService;

    private static final UUID DEPARTURE_HUB = UUID.randomUUID();

    private CompanyDeliveryRouteRecord newRecord(UUID deliveryId, UUID agentId) {
        return CompanyDeliveryRouteRecord.builder()
            .deliveryId(deliveryId)
            .departureHubId(DEPARTURE_HUB)
            .receiverCompanyId(UUID.randomUUID())
            .agentId(agentId)
            .build();
    }

    private void stubDeliveryExists(UUID deliveryId) {
        Delivery delivery = Delivery.builder()
            .orderId(UUID.randomUUID())
            .orderItemId(UUID.randomUUID())
            .departureHubId(UUID.randomUUID())
            .destinationHubId(DEPARTURE_HUB)
            .deliveryAddress("addr")
            .receiver("receiver")
            .build();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));
    }

    @Test
    @DisplayName("허브 관리자는 업체 배송 경로의 상태(진행 현황)는 변경할 수 없다(마스터/배송담당자 전용)")
    void blocksHubManagerFromStatusUpdate() {
        UUID deliveryId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        stubDeliveryExists(deliveryId);
        when(companyRouteRecordRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(recordId, deliveryId))
            .thenReturn(Optional.of(newRecord(deliveryId, UUID.randomUUID())));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> companyRouteRecordCommandService.updateStatus(deliveryId, recordId,
                UpdateCompanyRouteRecordCommand.builder().status(CompanyRouteRecordStatus.COMPANY_MOVING).build(),
                UserRole.HUB_MANAGER, UUID.randomUUID()));

        assertEquals(ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("배송 담당자는 본인이 배정되지 않은 업체 배송 경로는 상태를 변경할 수 없다")
    void blocksDeliveryManagerNotOwnAgentForStatusUpdate() {
        UUID deliveryId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        stubDeliveryExists(deliveryId);
        when(companyRouteRecordRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(recordId, deliveryId))
            .thenReturn(Optional.of(newRecord(deliveryId, UUID.randomUUID())));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> companyRouteRecordCommandService.updateStatus(deliveryId, recordId,
                UpdateCompanyRouteRecordCommand.builder().status(CompanyRouteRecordStatus.COMPANY_MOVING).build(),
                UserRole.DELIVERY_MANAGER, UUID.randomUUID()));

        assertEquals(ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("배송 완료(DELIVERED) 처리 시 실제 거리·소요시간이 없으면 거부된다")
    void requiresActualInfoOnDelivered() {
        UUID deliveryId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        stubDeliveryExists(deliveryId);
        CompanyDeliveryRouteRecord record = newRecord(deliveryId, UUID.randomUUID());
        record.update(CompanyRouteRecordStatus.COMPANY_MOVING, null, null);
        when(companyRouteRecordRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(recordId, deliveryId))
            .thenReturn(Optional.of(record));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> companyRouteRecordCommandService.updateStatus(deliveryId, recordId,
                UpdateCompanyRouteRecordCommand.builder().status(CompanyRouteRecordStatus.DELIVERED).build(),
                UserRole.MASTER, UUID.randomUUID()));

        assertEquals(ErrorCode.COMPANY_ROUTE_RECORD_ACTUAL_INFO_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("배송 담당자는 경로 계획(좌표/예상거리 등) 수동 보정 권한이 없다(마스터/허브 관리자 전용)")
    void blocksDeliveryManagerFromRoutePlanUpdate() {
        UUID deliveryId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        stubDeliveryExists(deliveryId);
        when(companyRouteRecordRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(recordId, deliveryId))
            .thenReturn(Optional.of(newRecord(deliveryId, UUID.randomUUID())));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> companyRouteRecordCommandService.updateRoutePlan(deliveryId, recordId,
                UpdateCompanyRouteRecordPlanCommand.builder().deliverySequence(1).build(),
                UserRole.DELIVERY_MANAGER, null));

        assertEquals(ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허브 관리자는 담당 허브(출발 허브)와 무관한 경로 계획은 보정할 수 없다")
    void blocksHubManagerOutsideHubForRoutePlanUpdate() {
        UUID deliveryId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        stubDeliveryExists(deliveryId);
        when(companyRouteRecordRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(recordId, deliveryId))
            .thenReturn(Optional.of(newRecord(deliveryId, UUID.randomUUID())));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> companyRouteRecordCommandService.updateRoutePlan(deliveryId, recordId,
                UpdateCompanyRouteRecordPlanCommand.builder().deliverySequence(1).build(),
                UserRole.HUB_MANAGER, UUID.randomUUID()));

        assertEquals(ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("마스터는 경로 계획을 정상적으로 수동 보정할 수 있다")
    void updatesRoutePlanSuccessfully() {
        UUID deliveryId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        stubDeliveryExists(deliveryId);
        when(companyRouteRecordRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(recordId, deliveryId))
            .thenReturn(Optional.of(newRecord(deliveryId, UUID.randomUUID())));

        UpdateCompanyRouteRecordPlanResponseDto result = companyRouteRecordCommandService.updateRoutePlan(
            deliveryId, recordId,
            UpdateCompanyRouteRecordPlanCommand.builder().deliverySequence(3).build(),
            UserRole.MASTER, null);

        assertEquals(3, result.deliverySequence());
    }
}