package com.logistics.delivery.command.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyRouteResequenceWriterTest {

    @Mock CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;
    @Mock DeliveryAgentRepository deliveryAgentRepository;
    @InjectMocks CompanyRouteResequenceWriter resequenceWriter;

    private static final UUID HUB_ID = UUID.randomUUID();
    private static final UUID AGENT_ID = UUID.randomUUID();

    private static void setId(CompanyDeliveryRouteRecord record, UUID id) {
        try {
            Field field = CompanyDeliveryRouteRecord.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(record, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private CompanyDeliveryRouteRecord waitingRecord(UUID id) {
        CompanyDeliveryRouteRecord record = CompanyDeliveryRouteRecord.builder()
            .deliveryId(UUID.randomUUID())
            .departureHubId(HUB_ID)
            .receiverCompanyId(UUID.randomUUID())
            .latitude(37.51)
            .longitude(127.00)
            .estimatedDistance(5)
            .estimatedDuration(10)
            .agentId(AGENT_ID)
            .build();
        setId(record, id);
        return record;
    }

    @Test
    @DisplayName("계산 시작 시점과 WAITING 목록이 동일하면 담당자 단위 advisory lock을 잡고 순서와 총 동선을 반영한다")
    void appliesSequenceAndRouteTotalWhenWaitingListUnchanged() {
        UUID nearId = UUID.randomUUID();
        UUID farId = UUID.randomUUID();
        CompanyDeliveryRouteRecord near = waitingRecord(nearId);
        CompanyDeliveryRouteRecord far = waitingRecord(farId);
        when(companyDeliveryRouteRecordRepository
            .findByAgentIdAndStatusAndDeletedAtIsNull(AGENT_ID, CompanyRouteRecordStatus.WAITING))
            .thenReturn(List.of(near, far));
        DeliveryAgent agent = DeliveryAgent.builder().agentId(AGENT_ID).agentType(AgentType.COMPANY_DELIVERY)
            .hubId(HUB_ID).deliveryOrder(0).build();
        when(deliveryAgentRepository.findByIdAndDeletedAtIsNull(AGENT_ID)).thenReturn(Optional.of(agent));

        resequenceWriter.apply(AGENT_ID, Set.of(nearId, farId), List.of(farId, nearId),
            new CompanyRouteResequenceWriter.RouteTotal(2, 2));

        verify(deliveryAgentRepository).lockAgentGroup("company-route-resequence:" + AGENT_ID);
        assertEquals(1, far.getDeliverySequence());
        assertEquals(2, near.getDeliverySequence());
        assertEquals(2, agent.getTotalDistance());
        assertEquals(2, agent.getTotalDuration());
    }

    @Test
    @DisplayName("계산 도중 WAITING 목록이 바뀌면 잠금만 잡고 반영은 건너뛴다")
    void skipsApplyWhenWaitingListChangedConcurrently() {
        UUID nearId = UUID.randomUUID();
        UUID staleFarId = UUID.randomUUID();
        UUID newlyAddedId = UUID.randomUUID();
        CompanyDeliveryRouteRecord near = waitingRecord(nearId);
        CompanyDeliveryRouteRecord newlyAdded = waitingRecord(newlyAddedId);
        // 계산이 끝난 지금은 staleFarId 대신 newlyAddedId가 WAITING 목록에 있음(동시에 다른 배정이 끼어든 상황)
        when(companyDeliveryRouteRecordRepository
            .findByAgentIdAndStatusAndDeletedAtIsNull(AGENT_ID, CompanyRouteRecordStatus.WAITING))
            .thenReturn(List.of(near, newlyAdded));

        resequenceWriter.apply(AGENT_ID, Set.of(nearId, staleFarId), List.of(staleFarId, nearId),
            new CompanyRouteResequenceWriter.RouteTotal(2, 2));

        verify(deliveryAgentRepository).lockAgentGroup("company-route-resequence:" + AGENT_ID);
        assertNull(near.getDeliverySequence());
        assertNull(newlyAdded.getDeliverySequence());
        verify(deliveryAgentRepository, never()).findByIdAndDeletedAtIsNull(eq(AGENT_ID));
    }

    @Test
    @DisplayName("총 동선 계산에 실패해 routeTotal이 null이면 순서는 반영하되 담당자 총 동선 갱신은 건너뛴다")
    void appliesSequenceOnlyWhenRouteTotalMissing() {
        UUID nearId = UUID.randomUUID();
        CompanyDeliveryRouteRecord near = waitingRecord(nearId);
        when(companyDeliveryRouteRecordRepository
            .findByAgentIdAndStatusAndDeletedAtIsNull(AGENT_ID, CompanyRouteRecordStatus.WAITING))
            .thenReturn(List.of(near));

        resequenceWriter.apply(AGENT_ID, Set.of(nearId), List.of(nearId), null);

        assertEquals(1, near.getDeliverySequence());
        verify(deliveryAgentRepository, never()).findByIdAndDeletedAtIsNull(eq(AGENT_ID));
    }
}
