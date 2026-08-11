package com.logistics.delivery.command.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
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
class DeliveryAgentAssignmentServiceTest {

    @Mock DeliveryAgentRepository deliveryAgentRepository;
    @Mock DeliveryRouteRecordRepository deliveryRouteRecordRepository;
    @Mock CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;
    @InjectMocks DeliveryAgentAssignmentService assignmentService;

    private DeliveryAgent agent(UUID id, Integer order) {
        return DeliveryAgent.builder()
            .agentId(id)
            .agentType(AgentType.HUB_DELIVERY)
            .deliveryOrder(order)
            .isAvailable(true)
            .build();
    }

    @Test
    @DisplayName("가용한 담당자가 한 명도 없으면 배정 자체가 불가능하다")
    void throwsWhenNoAvailableAgent() {
        when(deliveryAgentRepository
            .findByAgentTypeAndHubIdAndIsAvailableTrueAndDeletedAtIsNullOrderByDeliveryOrderAsc(
                AgentType.HUB_DELIVERY, null))
            .thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
            () -> assignmentService.assignNext(AgentType.HUB_DELIVERY, null));
        assertEquals(ErrorCode.DELIVERY_PERSON_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("직전에 배정된 담당자의 다음 순번(커서 이후 첫 담당자)이 배정된다")
    void assignsAgentAfterLastAssignedCursor() {
        DeliveryAgent order0 = agent(UUID.randomUUID(), 0);
        DeliveryAgent order1 = agent(UUID.randomUUID(), 1);
        DeliveryAgent order2 = agent(UUID.randomUUID(), 2);
        when(deliveryAgentRepository
            .findByAgentTypeAndHubIdAndIsAvailableTrueAndDeletedAtIsNullOrderByDeliveryOrderAsc(
                AgentType.HUB_DELIVERY, null))
            .thenReturn(List.of(order0, order1, order2));

        DeliveryRouteRecord lastAssigned = DeliveryRouteRecord.builder()
            .deliveryId(UUID.randomUUID())
            .sequence(1)
            .departureHubId(UUID.randomUUID())
            .arrivalHubId(UUID.randomUUID())
            .estimatedDistance(10)
            .estimatedDuration(10)
            .agentId(order0.getId())
            .build();
        when(deliveryRouteRecordRepository.findFirstByAgentIdIsNotNullOrderByCreatedAtDesc())
            .thenReturn(Optional.of(lastAssigned));
        when(deliveryAgentRepository.findById(order0.getId())).thenReturn(Optional.of(order0));

        DeliveryAgent next = assignmentService.assignNext(AgentType.HUB_DELIVERY, null);

        assertEquals(order1.getId(), next.getId());
    }

    @Test
    @DisplayName("마지막 순번 담당자 다음에는 순환해서 첫 번째 담당자로 되돌아간다")
    void wrapsAroundToFirstAgentWhenCursorIsLast() {
        DeliveryAgent order0 = agent(UUID.randomUUID(), 0);
        DeliveryAgent order1 = agent(UUID.randomUUID(), 1);
        when(deliveryAgentRepository
            .findByAgentTypeAndHubIdAndIsAvailableTrueAndDeletedAtIsNullOrderByDeliveryOrderAsc(
                AgentType.HUB_DELIVERY, null))
            .thenReturn(List.of(order0, order1));

        DeliveryRouteRecord lastAssigned = DeliveryRouteRecord.builder()
            .deliveryId(UUID.randomUUID())
            .sequence(1)
            .departureHubId(UUID.randomUUID())
            .arrivalHubId(UUID.randomUUID())
            .estimatedDistance(10)
            .estimatedDuration(10)
            .agentId(order1.getId())
            .build();
        when(deliveryRouteRecordRepository.findFirstByAgentIdIsNotNullOrderByCreatedAtDesc())
            .thenReturn(Optional.of(lastAssigned));
        when(deliveryAgentRepository.findById(order1.getId())).thenReturn(Optional.of(order1));

        DeliveryAgent next = assignmentService.assignNext(AgentType.HUB_DELIVERY, null);

        assertEquals(order0.getId(), next.getId());
    }

    @Test
    @DisplayName("이전 배정 이력이 전혀 없으면(최초 배정) 순번이 가장 빠른 담당자가 배정된다")
    void assignsFirstAgentWhenNoPriorAssignmentExists() {
        UUID hubId = UUID.randomUUID();
        DeliveryAgent order0 = DeliveryAgent.builder()
            .agentId(UUID.randomUUID())
            .hubId(hubId)
            .agentType(AgentType.COMPANY_DELIVERY)
            .deliveryOrder(0)
            .isAvailable(true)
            .build();
        when(deliveryAgentRepository
            .findByAgentTypeAndHubIdAndIsAvailableTrueAndDeletedAtIsNullOrderByDeliveryOrderAsc(
                AgentType.COMPANY_DELIVERY, hubId))
            .thenReturn(List.of(order0));
        when(companyDeliveryRouteRecordRepository
            .findFirstByDepartureHubIdAndAgentIdIsNotNullOrderByCreatedAtDesc(hubId))
            .thenReturn(Optional.empty());

        DeliveryAgent next = assignmentService.assignNext(AgentType.COMPANY_DELIVERY, hubId);

        assertEquals(order0.getId(), next.getId());
    }

    @Test
    @DisplayName("업체배송담당자(COMPANY_DELIVERY)는 hubId 없이는 배정 대상 자체를 조회하지 않고 거부된다")
    void rejectsCompanyDeliveryWithoutHubId() {
        assertThrows(BusinessException.class,
            () -> assignmentService.assignNext(AgentType.COMPANY_DELIVERY, null));
    }
}