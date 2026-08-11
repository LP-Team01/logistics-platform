package com.logistics.delivery.command.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.delivery.command.dto.command.CreateDeliveryCommand;
import com.logistics.delivery.domain.entity.DeliveryOrderCoordination;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryOrderCoordinationRepository;
import com.logistics.delivery.domain.repository.DeliveryRepository;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
import com.logistics.delivery.global.config.HubInternalServiceProperties;
import com.logistics.delivery.global.config.InternalServiceProperties;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.infrastructure.client.HubServiceClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryCommandServiceCompensationTest {

    @Mock DeliveryRepository deliveryRepository;
    @Mock DeliveryOrderCoordinationRepository coordinationRepository;
    @Mock DeliveryRouteRecordRepository deliveryRouteRecordRepository;
    @Mock CompanyDeliveryRouteRecordRepository companyRouteRecordRepository;
    @Mock DeliveryAgentAssignmentService deliveryAgentAssignmentService;
    @Mock HubServiceClient hubServiceClient;
    @Mock InternalServiceProperties internalServiceProperties;
    @Mock HubInternalServiceProperties hubInternalServiceProperties;
    @InjectMocks DeliveryCommandService deliveryCommandService;

    /** 배송이 아직 없어도 주문 취소 의도를 먼저 저장합니다. */
    @Test
    void recordsCancellationWhenDeliveryDoesNotExistYet() {
        UUID orderId = UUID.randomUUID();
        DeliveryOrderCoordination coordination = mock(DeliveryOrderCoordination.class);
        when(coordinationRepository.findByOrderIdForUpdate(orderId))
            .thenReturn(Optional.of(coordination));
        when(deliveryRepository.findByOrderIdAndDeletedAtIsNull(orderId))
            .thenReturn(List.of());

        deliveryCommandService.cancelByOrderId(orderId);

        verify(coordinationRepository).ensureExists(orderId);
        verify(coordination).cancel();
    }

    /** 취소가 먼저 접수된 주문은 뒤늦은 배송 생성을 거부합니다. */
    @Test
    void rejectsDeliveryCreationForCancelledOrder() {
        UUID orderId = UUID.randomUUID();
        CreateDeliveryCommand command = mock(CreateDeliveryCommand.class);
        DeliveryOrderCoordination coordination = mock(DeliveryOrderCoordination.class);
        when(command.orderId()).thenReturn(orderId);
        when(coordinationRepository.findByOrderIdForUpdate(orderId))
            .thenReturn(Optional.of(coordination));
        when(coordination.isCancelled()).thenReturn(true);

        assertThrows(
            BusinessException.class,
            () -> deliveryCommandService.create(command)
        );

        verify(coordinationRepository).ensureExists(orderId);
    }
}
