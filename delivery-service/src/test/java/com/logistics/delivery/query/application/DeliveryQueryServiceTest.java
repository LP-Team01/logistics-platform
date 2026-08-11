package com.logistics.delivery.query.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryRepository;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.query.dto.response.DeliveryDetailResponseDto;
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
class DeliveryQueryServiceTest {

    @Mock DeliveryRepository deliveryRepository;
    @Mock DeliveryRouteRecordRepository deliveryRouteRecordRepository;
    @Mock CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;
    @InjectMocks DeliveryQueryService deliveryQueryService;

    private static final UUID DEPARTURE_HUB = UUID.randomUUID();
    private static final UUID DESTINATION_HUB = UUID.randomUUID();

    private void stubDelivery(UUID deliveryId, Delivery delivery) {
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(deliveryId))
            .thenReturn(List.of());
    }

    private Delivery newDelivery() {
        return Delivery.builder()
            .orderId(UUID.randomUUID())
            .orderItemId(UUID.randomUUID())
            .departureHubId(DEPARTURE_HUB)
            .destinationHubId(DESTINATION_HUB)
            .deliveryAddress("addr")
            .receiver("receiver")
            .build();
    }

    @Test
    @DisplayName("업체 담당자는 본인 업체가 수령업체인 배송만 조회할 수 있다")
    void blocksCompanyManagerForOtherCompany() {
        UUID deliveryId = UUID.randomUUID();
        stubDelivery(deliveryId, newDelivery());
        when(companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(any()))
            .thenReturn(Optional.of(CompanyDeliveryRouteRecord.builder()
                .deliveryId(deliveryId).departureHubId(DESTINATION_HUB).receiverCompanyId(UUID.randomUUID())
                .build()));

        BusinessException exception = assertThrows(BusinessException.class, () -> deliveryQueryService.getDelivery(
            UserRole.COMPANY_MANAGER, UUID.randomUUID(), null, UUID.randomUUID(), deliveryId));

        assertEquals(ErrorCode.DELIVERY_QUERY_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("업체 담당자는 본인 업체의 수령 건이면 정상 조회할 수 있다")
    void allowsCompanyManagerForOwnCompany() {
        UUID deliveryId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        stubDelivery(deliveryId, newDelivery());
        when(companyDeliveryRouteRecordRepository.findByDeliveryIdAndDeletedAtIsNull(any()))
            .thenReturn(Optional.of(CompanyDeliveryRouteRecord.builder()
                .deliveryId(deliveryId).departureHubId(DESTINATION_HUB).receiverCompanyId(companyId)
                .build()));

        DeliveryDetailResponseDto result = deliveryQueryService.getDelivery(
            UserRole.COMPANY_MANAGER, UUID.randomUUID(), null, companyId, deliveryId);

        assertEquals(DEPARTURE_HUB, result.departureHubId());
    }

    @Test
    @DisplayName("배송 담당자는 본인이 배정되지 않은 배송은 조회할 수 없다")
    void blocksDeliveryManagerNotAssigned() {
        UUID deliveryId = UUID.randomUUID();
        stubDelivery(deliveryId, newDelivery());

        BusinessException exception = assertThrows(BusinessException.class, () -> deliveryQueryService.getDelivery(
            UserRole.DELIVERY_MANAGER, UUID.randomUUID(), null, null, deliveryId));

        assertEquals(ErrorCode.DELIVERY_QUERY_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허브 관리자는 담당 허브(출발/도착)와 무관한 배송은 조회할 수 없다")
    void blocksHubManagerOutsideHub() {
        UUID deliveryId = UUID.randomUUID();
        stubDelivery(deliveryId, newDelivery());

        BusinessException exception = assertThrows(BusinessException.class, () -> deliveryQueryService.getDelivery(
            UserRole.HUB_MANAGER, UUID.randomUUID(), UUID.randomUUID(), null, deliveryId));

        assertEquals(ErrorCode.DELIVERY_QUERY_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허브 관리자가 X-Hub-Id 헤더 없이 목록을 검색하면 \"전체 허용\"으로 새지 않고 거부해야 한다(fail-closed)")
    void rejectsHubManagerSearchWithoutHubHeader() {
        BusinessException exception = assertThrows(BusinessException.class, () -> deliveryQueryService.searchDelivery(
            UserRole.HUB_MANAGER, UUID.randomUUID(), null, null, null, null));

        assertEquals(ErrorCode.DELIVERY_QUERY_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("업체 담당자가 X-Company-Id 헤더 없이 목록을 검색하면 거부해야 한다(fail-closed)")
    void rejectsCompanyManagerSearchWithoutCompanyHeader() {
        BusinessException exception = assertThrows(BusinessException.class, () -> deliveryQueryService.searchDelivery(
            UserRole.COMPANY_MANAGER, UUID.randomUUID(), null, null, null, null));

        assertEquals(ErrorCode.DELIVERY_QUERY_FORBIDDEN, exception.getErrorCode());
    }
}