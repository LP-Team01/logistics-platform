package com.logistics.delivery.domain.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.global.config.JpaAuditingConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class DeliveryAgentSpecificationTest {

    @Autowired TestEntityManager entityManager;
    @Autowired DeliveryAgentRepository deliveryAgentRepository;

    private static final UUID HUB_A = UUID.randomUUID();
    private static final UUID HUB_B = UUID.randomUUID();

    private DeliveryAgent persistAgent(AgentType agentType, UUID hubId, int order, boolean available) {
        DeliveryAgent agent = DeliveryAgent.builder()
            .agentId(UUID.randomUUID())
            .hubId(hubId)
            .agentType(agentType)
            .deliveryOrder(order)
            .isAvailable(available)
            .build();
        return entityManager.persistAndFlush(agent);
    }

    @Test
    @DisplayName("hubId 조건으로 필터링하면 해당 허브 소속 담당자만 조회된다")
    void filtersByHubId() {
        DeliveryAgent hubAAgent = persistAgent(AgentType.COMPANY_DELIVERY, HUB_A, 0, true);
        persistAgent(AgentType.COMPANY_DELIVERY, HUB_B, 0, true);

        Specification<DeliveryAgent> spec = DeliveryAgentSpecification.withSearchCondition(
            HUB_A, null, null, null);
        List<DeliveryAgent> result = deliveryAgentRepository.findAll(spec);

        assertEquals(1, result.size());
        assertEquals(hubAAgent.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("agentType 조건으로 필터링하면 해당 유형의 담당자만 조회된다")
    void filtersByAgentType() {
        persistAgent(AgentType.HUB_DELIVERY, null, 0, true);
        persistAgent(AgentType.COMPANY_DELIVERY, HUB_A, 0, true);

        Specification<DeliveryAgent> spec = DeliveryAgentSpecification.withSearchCondition(
            null, AgentType.HUB_DELIVERY, null, null);
        List<DeliveryAgent> result = deliveryAgentRepository.findAll(spec);

        assertEquals(1, result.size());
        assertEquals(AgentType.HUB_DELIVERY, result.get(0).getAgentType());
    }

    @Test
    @DisplayName("isAvailable 조건으로 필터링하면 가용 여부가 일치하는 담당자만 조회된다")
    void filtersByAvailability() {
        persistAgent(AgentType.HUB_DELIVERY, null, 0, true);
        DeliveryAgent unavailable = persistAgent(AgentType.HUB_DELIVERY, null, 1, false);

        Specification<DeliveryAgent> spec = DeliveryAgentSpecification.withSearchCondition(
            null, null, false, null);
        List<DeliveryAgent> result = deliveryAgentRepository.findAll(spec);

        assertEquals(1, result.size());
        assertEquals(unavailable.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("agentId 조건으로 필터링하면 본인 정보만 조회된다(배송 담당자 본인 조회 스코핑)")
    void filtersByAgentId() {
        DeliveryAgent self = persistAgent(AgentType.HUB_DELIVERY, null, 0, true);
        persistAgent(AgentType.HUB_DELIVERY, null, 1, true);

        Specification<DeliveryAgent> spec = DeliveryAgentSpecification.withSearchCondition(
            null, null, null, self.getId());
        List<DeliveryAgent> result = deliveryAgentRepository.findAll(spec);

        assertEquals(1, result.size());
        assertEquals(self.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("논리 삭제된 담당자는 결과에서 제외된다")
    void excludesDeletedAgents() {
        persistAgent(AgentType.HUB_DELIVERY, null, 0, true);
        DeliveryAgent deleted = persistAgent(AgentType.HUB_DELIVERY, null, 1, true);
        deleted.softDelete(UUID.randomUUID());
        entityManager.persistAndFlush(deleted);

        Specification<DeliveryAgent> spec = DeliveryAgentSpecification.withSearchCondition(
            null, null, null, null);
        List<DeliveryAgent> result = deliveryAgentRepository.findAll(spec);

        assertEquals(1, result.size());
    }
}