package com.logistics.order.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryCompensationOutboxServiceTest {

    @Mock
    private DeliveryCompensationOutboxRepository repository;

    @Test
    void savesOnlyOncePerOrder() {
        UUID orderId = UUID.randomUUID();
        DeliveryCompensationOutbox existing = new DeliveryCompensationOutbox(orderId);
        when(repository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        new DeliveryCompensationOutboxService(repository).save(orderId);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    /**
     * 처음 발생한 배송 보상 실패는 Outbox에 저장되는지 검증합니다.
     */
    @Test
    void savesOutboxWhenOrderDoesNotExist() {
        UUID orderId = UUID.randomUUID();

        // 같은 주문의 기존 Outbox가 없는 상황
        when(repository.findByOrderId(orderId))
                .thenReturn(Optional.empty());

        new DeliveryCompensationOutboxService(
                repository
        ).save(orderId);

        // Repository에 전달된 Outbox 객체 확인
        ArgumentCaptor<DeliveryCompensationOutbox> captor =
                ArgumentCaptor.forClass(
                        DeliveryCompensationOutbox.class
                );

        verify(repository).save(
                captor.capture()
        );

        DeliveryCompensationOutbox savedOutbox =
                captor.getValue();

        assertNotNull(savedOutbox.getEventId());
        assertEquals(
                orderId,
                savedOutbox.getOrderId()
        );
        assertNotNull(savedOutbox.getCreatedAt());

        // 아직 Kafka에 발행되지 않았으므로 NULL
        assertNull(savedOutbox.getPublishedAt());
    }
}
