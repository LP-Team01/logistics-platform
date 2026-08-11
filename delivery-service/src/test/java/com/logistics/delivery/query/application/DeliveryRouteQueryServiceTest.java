package com.logistics.delivery.query.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
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
class DeliveryRouteQueryServiceTest {

    @Mock DeliveryRouteRecordRepository deliveryRouteRecordRepository;
    @Mock CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;
    @InjectMocks DeliveryRouteQueryService deliveryRouteQueryService;

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

    @Test
    @DisplayName("경로 기록이 하나도 없으면 404로 처리된다")
    void throwsNotFoundWhenNoRouteRecords() {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId))
            .thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryRouteQueryService.getRouteRecords(
                UserRole.MASTER, UUID.randomUUID(), null, null, deliveryId));

        assertEquals(ErrorCode.DELIVERY_ROUTE_RECORD_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("배송 담당자는 본인이 배정된 구간이 없는 배송의 경로 기록은 조회할 수 없다")
    void blocksDeliveryManagerNotAssigned() {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId))
            .thenReturn(List.of(newRouteRecord(deliveryId, UUID.randomUUID())));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryRouteQueryService.getRouteRecords(
                UserRole.DELIVERY_MANAGER, UUID.randomUUID(), null, null, deliveryId));

        assertEquals(ErrorCode.DELIVERY_ROUTE_RECORD_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허브 관리자는 담당 허브와 무관한 경로 기록은 조회할 수 없다")
    void blocksHubManagerOutsideHub() {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId))
            .thenReturn(List.of(newRouteRecord(deliveryId, UUID.randomUUID())));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryRouteQueryService.getRouteRecords(
                UserRole.HUB_MANAGER, UUID.randomUUID(), UUID.randomUUID(), null, deliveryId));

        assertEquals(ErrorCode.DELIVERY_ROUTE_RECORD_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("업체 담당자는 본인 업체가 수령업체가 아닌 배송의 경로 기록은 조회할 수 없다")
    void blocksCompanyManagerForOtherCompany() {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId))
            .thenReturn(List.of(newRouteRecord(deliveryId, UUID.randomUUID())));
        when(companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(any()))
            .thenReturn(Optional.of(CompanyDeliveryRouteRecord.builder()
                .deliveryId(deliveryId).departureHubId(ARRIVAL_HUB).receiverCompanyId(UUID.randomUUID())
                .build()));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryRouteQueryService.getRouteRecords(
                UserRole.COMPANY_MANAGER, UUID.randomUUID(), null, UUID.randomUUID(), deliveryId));

        assertEquals(ErrorCode.DELIVERY_ROUTE_RECORD_FORBIDDEN, exception.getErrorCode());
    }
}