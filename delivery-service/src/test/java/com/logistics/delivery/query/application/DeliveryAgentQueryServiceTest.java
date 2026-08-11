package com.logistics.delivery.query.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.logistics.delivery.command.application.DeliveryAgentAssignmentService;
import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.query.dto.request.DeliveryAgentSearchRequestDto;
import com.logistics.delivery.query.dto.response.DeliveryAgentDetailResponseDto;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryAgentQueryServiceTest {

    @Mock DeliveryAgentRepository deliveryAgentRepository;
    @Mock DeliveryAgentAssignmentService deliveryAgentAssignmentService;
    @Mock CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;
    @InjectMocks DeliveryAgentQueryService deliveryAgentQueryService;

    private DeliveryAgent agent(UUID id, AgentType agentType, UUID hubId) {
        return DeliveryAgent.builder()
            .agentId(id)
            .agentType(agentType)
            .hubId(hubId)
            .deliveryOrder(0)
            .build();
    }

    @Test
    @DisplayName("업체 담당자는 배송담당자 API에 접근할 권한이 아예 없다")
    void blocksCompanyManagerFromSearching() {
        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryAgentQueryService.searchDeliveryAgents(
                UserRole.COMPANY_MANAGER, UUID.randomUUID(), null, null, null));

        assertEquals(ErrorCode.DELIVERY_AGENT_QUERY_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허브 관리자가 X-Hub-Id 헤더 없이 검색하면 \"전체 허용\"으로 새지 않고 거부해야 한다(fail-closed)")
    void rejectsHubManagerSearchWithoutHubHeader() {
        DeliveryAgentSearchRequestDto request = new DeliveryAgentSearchRequestDto(null, null, null);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryAgentQueryService.searchDeliveryAgents(
                UserRole.HUB_MANAGER, UUID.randomUUID(), null, request, null));

        assertEquals(ErrorCode.DELIVERY_AGENT_QUERY_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("배송 담당자는 본인이 아닌 다른 담당자의 정보는 조회할 수 없다")
    void blocksDeliveryManagerRequestingOtherAgent() {
        UUID agentId = UUID.randomUUID();
        when(deliveryAgentRepository.findByIdAndDeletedAtIsNull(agentId))
            .thenReturn(Optional.of(agent(agentId, AgentType.HUB_DELIVERY, null)));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryAgentQueryService.getDeliveryAgent(
                UserRole.DELIVERY_MANAGER, UUID.randomUUID(), null, agentId));

        assertEquals(ErrorCode.DELIVERY_AGENT_QUERY_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허브 배송 담당자(전체 풀)는 허브 관리자의 관리/조회 범위 밖이다")
    void blocksHubManagerForHubDeliveryTypeAgent() {
        UUID agentId = UUID.randomUUID();
        when(deliveryAgentRepository.findByIdAndDeletedAtIsNull(agentId))
            .thenReturn(Optional.of(agent(agentId, AgentType.HUB_DELIVERY, null)));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryAgentQueryService.getDeliveryAgent(
                UserRole.HUB_MANAGER, UUID.randomUUID(), UUID.randomUUID(), agentId));

        assertEquals(ErrorCode.DELIVERY_AGENT_QUERY_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허브 관리자는 담당 허브가 아닌 곳 소속의 업체배송담당자는 조회할 수 없다")
    void blocksHubManagerOutsideHubForCompanyDeliveryAgent() {
        UUID agentId = UUID.randomUUID();
        UUID agentHub = UUID.randomUUID();
        when(deliveryAgentRepository.findByIdAndDeletedAtIsNull(agentId))
            .thenReturn(Optional.of(agent(agentId, AgentType.COMPANY_DELIVERY, agentHub)));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryAgentQueryService.getDeliveryAgent(
                UserRole.HUB_MANAGER, UUID.randomUUID(), UUID.randomUUID(), agentId));

        assertEquals(ErrorCode.DELIVERY_AGENT_QUERY_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("배송 담당자는 본인 정보는 정상적으로 조회할 수 있다")
    void allowsDeliveryManagerRequestingOwnAgent() {
        UUID agentId = UUID.randomUUID();
        when(deliveryAgentRepository.findByIdAndDeletedAtIsNull(agentId))
            .thenReturn(Optional.of(agent(agentId, AgentType.HUB_DELIVERY, null)));

        DeliveryAgentDetailResponseDto result = deliveryAgentQueryService.getDeliveryAgent(
            UserRole.DELIVERY_MANAGER, agentId, null, agentId);

        assertEquals(agentId, result.agentId());
    }
}