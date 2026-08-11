package com.logistics.delivery.domain.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.DeliveryStatus;
import com.logistics.delivery.global.config.JpaAuditingConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class DeliverySpecificationTest {

    @Autowired TestEntityManager entityManager;
    @Autowired DeliveryRepository deliveryRepository;
    @Autowired DeliveryRouteRecordRepository deliveryRouteRecordRepository;
    @Autowired CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;

    private static final UUID HUB_A = UUID.randomUUID();
    private static final UUID HUB_B = UUID.randomUUID();
    private static final UUID HUB_C = UUID.randomUUID();

    private Delivery persistDelivery(UUID departureHubId, UUID destinationHubId) {
        Delivery delivery = Delivery.builder()
            .orderId(UUID.randomUUID())
            .orderItemId(UUID.randomUUID())
            .departureHubId(departureHubId)
            .destinationHubId(destinationHubId)
            .deliveryAddress("addr")
            .receiver("receiver")
            .build();
        return entityManager.persistAndFlush(delivery);
    }

    @Test
    @DisplayName("status 조건으로 필터링하면 해당 상태의 배송만 조회된다")
    void filtersByStatus() {
        Delivery waiting = persistDelivery(HUB_A, HUB_B);
        Delivery moving = persistDelivery(HUB_A, HUB_B);
        moving.update(DeliveryStatus.HUB_MOVING);
        entityManager.persistAndFlush(moving);

        Specification<Delivery> spec = DeliverySpecification.withSearchCondition(
            DeliveryStatus.HUB_MOVING, null, null, null, null, null, null);
        List<Delivery> result = deliveryRepository.findAll(spec);

        assertEquals(1, result.size());
        assertEquals(moving.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("orderId 조건으로 필터링하면 해당 주문의 배송만 조회된다")
    void filtersByOrderId() {
        Delivery target = persistDelivery(HUB_A, HUB_B);
        persistDelivery(HUB_A, HUB_B);

        Specification<Delivery> spec = DeliverySpecification.withSearchCondition(
            null, target.getOrderId(), null, null, null, null, null);
        List<Delivery> result = deliveryRepository.findAll(spec);

        assertEquals(1, result.size());
        assertEquals(target.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("hubId 스코핑은 출발 허브 또는 도착 허브가 일치하는 배송만 포함한다")
    void scopesWithinHub() {
        Delivery departureMatch = persistDelivery(HUB_A, HUB_C);
        Delivery destinationMatch = persistDelivery(HUB_C, HUB_A);
        persistDelivery(HUB_B, HUB_C);

        Specification<Delivery> spec = DeliverySpecification.withSearchCondition(
            null, null, null, null, null, HUB_A, null);
        List<Delivery> result = deliveryRepository.findAll(spec);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(d -> d.getId().equals(departureMatch.getId())));
        assertTrue(result.stream().anyMatch(d -> d.getId().equals(destinationMatch.getId())));
    }

    @Test
    @DisplayName("담당자 스코핑은 업체담당자로 배정됐거나 허브 구간에 배정된 배송을 모두 포함한다")
    void scopesAssignedToAgent() {
        UUID companyAgentId = UUID.randomUUID();
        UUID routeAgentId = UUID.randomUUID();
        Delivery viaCompanyAgent = Delivery.builder()
            .orderId(UUID.randomUUID())
            .orderItemId(UUID.randomUUID())
            .departureHubId(HUB_A)
            .destinationHubId(HUB_B)
            .deliveryAddress("addr")
            .receiver("receiver")
            .companyAgentId(companyAgentId)
            .build();
        entityManager.persistAndFlush(viaCompanyAgent);

        Delivery viaRouteAgent = persistDelivery(HUB_A, HUB_B);
        DeliveryRouteRecord routeRecord = DeliveryRouteRecord.builder()
            .deliveryId(viaRouteAgent.getId())
            .sequence(1)
            .departureHubId(HUB_A)
            .arrivalHubId(HUB_B)
            .estimatedDistance(10)
            .estimatedDuration(10)
            .agentId(routeAgentId)
            .build();
        entityManager.persistAndFlush(routeRecord);

        persistDelivery(HUB_A, HUB_B);

        Specification<Delivery> companySpec = DeliverySpecification.withSearchCondition(
            null, null, null, null, companyAgentId, null, null);
        assertEquals(1, deliveryRepository.findAll(companySpec).size());

        Specification<Delivery> routeSpec = DeliverySpecification.withSearchCondition(
            null, null, null, null, routeAgentId, null, null);
        List<Delivery> routeResult = deliveryRepository.findAll(routeSpec);
        assertEquals(1, routeResult.size());
        assertEquals(viaRouteAgent.getId(), routeResult.get(0).getId());
    }

    @Test
    @DisplayName("업체 스코핑은 요청자 소속 업체가 수령업체인 배송만 포함한다")
    void scopesForReceiverCompany() {
        UUID companyId = UUID.randomUUID();
        Delivery delivery = persistDelivery(HUB_A, HUB_B);
        CompanyDeliveryRouteRecord companyRouteRecord = CompanyDeliveryRouteRecord.builder()
            .deliveryId(delivery.getId())
            .departureHubId(HUB_B)
            .receiverCompanyId(companyId)
            .build();
        entityManager.persistAndFlush(companyRouteRecord);
        persistDelivery(HUB_A, HUB_B);

        Specification<Delivery> spec = DeliverySpecification.withSearchCondition(
            null, null, null, null, null, null, companyId);
        List<Delivery> result = deliveryRepository.findAll(spec);

        assertEquals(1, result.size());
        assertEquals(delivery.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("논리 삭제된 배송은 결과에서 제외되고, 페이징 정보도 정확히 계산된다")
    void excludesDeletedAndPagesCorrectly() {
        persistDelivery(HUB_A, HUB_B);
        persistDelivery(HUB_A, HUB_B);
        Delivery deleted = persistDelivery(HUB_A, HUB_B);
        deleted.softDelete(UUID.randomUUID());
        entityManager.persistAndFlush(deleted);

        Specification<Delivery> spec = DeliverySpecification.withSearchCondition(
            null, null, null, null, null, null, null);
        Page<Delivery> firstPage = deliveryRepository.findAll(spec, PageRequest.of(0, 1));

        assertEquals(2, firstPage.getTotalElements());
        assertEquals(2, firstPage.getTotalPages());
        assertEquals(1, firstPage.getContent().size());
    }
}