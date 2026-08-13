package com.logistics.delivery.domain.repository;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.DeliveryStatus;
import com.logistics.delivery.domain.entity.RouteRecordStatus;
import com.logistics.delivery.global.config.JpaAuditingConfig;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

// Delivery/CompanyDeliveryRouteRecord의 @Version이 실제로 동시 상태 변경을 막는지 검증
// (두 트랜잭션이 같은 행을 각자 읽은 뒤, 먼저 커밋한 쪽만 성공하고 늦은 쪽은 충돌 예외로 거부되어야 함)
@DataJpaTest
@Import(JpaAuditingConfig.class)
class DeliveryOptimisticLockTest {

    @Autowired TestEntityManager entityManager;
    @Autowired DeliveryRepository deliveryRepository;
    @Autowired DeliveryRouteRecordRepository deliveryRouteRecordRepository;
    @Autowired CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;

    @Test
    @DisplayName("Delivery - 오래된 버전을 읽은 쪽이 뒤늦게 저장하면 낙관적 락 충돌 예외가 발생한다")
    void deliveryStaleWriteFailsWithOptimisticLock() {
        Delivery delivery = Delivery.builder()
            .orderId(UUID.randomUUID())
            .orderItemId(UUID.randomUUID())
            .departureHubId(UUID.randomUUID())
            .destinationHubId(UUID.randomUUID())
            .deliveryAddress("addr")
            .receiver("receiver")
            .build();
        UUID deliveryId = entityManager.persistAndFlush(delivery).getId();
        entityManager.clear();

        Delivery staleCopy = deliveryRepository.findById(deliveryId).orElseThrow();
        entityManager.detach(staleCopy);

        Delivery freshCopy = deliveryRepository.findById(deliveryId).orElseThrow();
        freshCopy.update(DeliveryStatus.HUB_MOVING);
        deliveryRepository.saveAndFlush(freshCopy);
        entityManager.clear();

        staleCopy.update(DeliveryStatus.HUB_MOVING);
        assertThrows(ObjectOptimisticLockingFailureException.class,
            () -> deliveryRepository.saveAndFlush(staleCopy));
    }

    @Test
    @DisplayName("DeliveryRouteRecord - HUB_MANAGER와 DELIVERY_MANAGER가 같은 경로기록을 동시에 갱신하면 늦은 쪽이 충돌 예외로 거부된다")
    void deliveryRouteRecordStaleWriteFailsWithOptimisticLock() {
        DeliveryRouteRecord routeRecord = DeliveryRouteRecord.builder()
            .deliveryId(UUID.randomUUID())
            .sequence(1)
            .departureHubId(UUID.randomUUID())
            .arrivalHubId(UUID.randomUUID())
            .estimatedDistance(10)
            .estimatedDuration(20)
            .build();
        UUID recordId = entityManager.persistAndFlush(routeRecord).getId();
        entityManager.clear();

        DeliveryRouteRecord staleCopy = deliveryRouteRecordRepository.findById(recordId).orElseThrow();
        entityManager.detach(staleCopy);

        DeliveryRouteRecord freshCopy = deliveryRouteRecordRepository.findById(recordId).orElseThrow();
        freshCopy.update(RouteRecordStatus.MOVING, null, null);
        deliveryRouteRecordRepository.saveAndFlush(freshCopy);
        entityManager.clear();

        staleCopy.update(RouteRecordStatus.MOVING, null, null);
        assertThrows(ObjectOptimisticLockingFailureException.class,
            () -> deliveryRouteRecordRepository.saveAndFlush(staleCopy));
    }

    @Test
    @DisplayName("CompanyDeliveryRouteRecord - 방문순서 재계산과 상태 변경이 같은 행을 동시에 건드리면 늦은 쪽이 충돌 예외로 거부된다")
    void companyRouteRecordStaleWriteFailsWithOptimisticLock() {
        CompanyDeliveryRouteRecord record = CompanyDeliveryRouteRecord.builder()
            .deliveryId(UUID.randomUUID())
            .departureHubId(UUID.randomUUID())
            .receiverCompanyId(UUID.randomUUID())
            .build();
        UUID recordId = entityManager.persistAndFlush(record).getId();
        entityManager.clear();

        CompanyDeliveryRouteRecord staleCopy = companyDeliveryRouteRecordRepository.findById(recordId).orElseThrow();
        entityManager.detach(staleCopy);

        CompanyDeliveryRouteRecord freshCopy = companyDeliveryRouteRecordRepository.findById(recordId).orElseThrow();
        freshCopy.updateSequence(2);
        companyDeliveryRouteRecordRepository.saveAndFlush(freshCopy);
        entityManager.clear();

        staleCopy.update(CompanyRouteRecordStatus.COMPANY_MOVING, null, null);
        assertThrows(ObjectOptimisticLockingFailureException.class,
            () -> companyDeliveryRouteRecordRepository.saveAndFlush(staleCopy));
    }
}