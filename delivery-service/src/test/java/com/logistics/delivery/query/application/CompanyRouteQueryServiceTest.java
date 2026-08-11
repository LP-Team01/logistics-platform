package com.logistics.delivery.query.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.query.dto.response.CompanyRouteResponseDto;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyRouteQueryServiceTest {

    @Mock CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;
    @InjectMocks CompanyRouteQueryService companyRouteQueryService;

    private static final UUID DEPARTURE_HUB = UUID.randomUUID();

    private CompanyDeliveryRouteRecord newRecord(UUID deliveryId, UUID agentId, UUID receiverCompanyId) {
        return CompanyDeliveryRouteRecord.builder()
            .deliveryId(deliveryId)
            .departureHubId(DEPARTURE_HUB)
            .receiverCompanyId(receiverCompanyId)
            .agentId(agentId)
            .build();
    }

    @Test
    @DisplayName("업체 배송 경로 기록이 없으면 404로 처리된다")
    void throwsNotFoundWhenRecordMissing() {
        UUID deliveryId = UUID.randomUUID();
        when(companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(deliveryId))
            .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
            () -> companyRouteQueryService.getCompanyRouteRecords(
                UserRole.MASTER, UUID.randomUUID(), null, null, deliveryId));

        assertEquals(ErrorCode.COMPANY_ROUTE_RECORD_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("배송 담당자는 본인이 배정되지 않은 업체 배송 경로는 조회할 수 없다")
    void blocksDeliveryManagerNotOwnAgent() {
        UUID deliveryId = UUID.randomUUID();
        when(companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(deliveryId))
            .thenReturn(Optional.of(newRecord(deliveryId, UUID.randomUUID(), UUID.randomUUID())));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> companyRouteQueryService.getCompanyRouteRecords(
                UserRole.DELIVERY_MANAGER, UUID.randomUUID(), null, null, deliveryId));

        assertEquals(ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허브 관리자는 담당 허브(출발 허브)와 무관한 업체 배송 경로는 조회할 수 없다")
    void blocksHubManagerOutsideHub() {
        UUID deliveryId = UUID.randomUUID();
        when(companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(deliveryId))
            .thenReturn(Optional.of(newRecord(deliveryId, UUID.randomUUID(), UUID.randomUUID())));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> companyRouteQueryService.getCompanyRouteRecords(
                UserRole.HUB_MANAGER, UUID.randomUUID(), UUID.randomUUID(), null, deliveryId));

        assertEquals(ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("업체 담당자는 본인 업체가 수령업체가 아니면 조회할 수 없다")
    void blocksCompanyManagerForOtherCompany() {
        UUID deliveryId = UUID.randomUUID();
        when(companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(deliveryId))
            .thenReturn(Optional.of(newRecord(deliveryId, UUID.randomUUID(), UUID.randomUUID())));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> companyRouteQueryService.getCompanyRouteRecords(
                UserRole.COMPANY_MANAGER, UUID.randomUUID(), null, UUID.randomUUID(), deliveryId));

        assertEquals(ErrorCode.COMPANY_ROUTE_RECORD_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("마스터는 항상 조회할 수 있다")
    void allowsMaster() {
        UUID deliveryId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();
        when(companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(deliveryId))
            .thenReturn(Optional.of(newRecord(deliveryId, UUID.randomUUID(), receiverCompanyId)));

        CompanyRouteResponseDto result = companyRouteQueryService.getCompanyRouteRecords(
            UserRole.MASTER, UUID.randomUUID(), null, null, deliveryId);

        assertEquals(receiverCompanyId, result.receiverCompanyId());
    }
}