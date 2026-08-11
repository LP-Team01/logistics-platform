package com.logistics.delivery.command.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.logistics.delivery.command.dto.command.CreateDeliveryAgentCommand;
import com.logistics.delivery.command.dto.command.UpdateDeliveryAgentCommand;
import com.logistics.delivery.command.dto.response.CreateDeliveryAgentResponseDto;
import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.common.UserStatus;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.infrastructure.client.HubQueryService;
import com.logistics.delivery.infrastructure.client.UserServiceClient;
import com.logistics.delivery.infrastructure.client.dto.UserServiceUserResponseDto;
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
class DeliveryAgentCommandServiceTest {

    @Mock DeliveryAgentRepository deliveryAgentRepository;
    @Mock UserServiceClient userServiceClient;
    @Mock HubQueryService hubQueryService;
    @InjectMocks DeliveryAgentCommandService deliveryAgentCommandService;

    private void stubApprovedDeliveryManager(UUID agentId) {
        lenient().when(userServiceClient.getUser(agentId)).thenReturn(
            new UserServiceUserResponseDto(agentId, UserRole.DELIVERY_MANAGER, UserStatus.APPROVED));
    }

    @Test
    @DisplayName("허브 배송 담당자(전체 풀)는 허브 관리자의 관리 범위 밖이라 생성할 수 없다")
    void blocksHubManagerCreatingHubDeliveryAgent() {
        CreateDeliveryAgentCommand command = CreateDeliveryAgentCommand.builder()
            .agentId(UUID.randomUUID())
            .agentType(AgentType.HUB_DELIVERY)
            .build();

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryAgentCommandService.create(command, UserRole.HUB_MANAGER, UUID.randomUUID()));

        assertEquals(ErrorCode.DELIVERY_AGENT_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허브 관리자는 담당 허브가 아닌 곳에 업체배송담당자를 생성할 수 없다")
    void blocksHubManagerCreatingOutsideOwnHub() {
        UUID targetHub = UUID.randomUUID();
        UUID requesterHub = UUID.randomUUID();
        CreateDeliveryAgentCommand command = CreateDeliveryAgentCommand.builder()
            .agentId(UUID.randomUUID())
            .agentType(AgentType.COMPANY_DELIVERY)
            .hubId(targetHub)
            .build();

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryAgentCommandService.create(command, UserRole.HUB_MANAGER, requesterHub));

        assertEquals(ErrorCode.DELIVERY_AGENT_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("업체배송담당자는 소속 허브(hubId)가 없으면 등록할 수 없다")
    void rejectsCompanyDeliveryWithoutHubId() {
        UUID agentId = UUID.randomUUID();
        stubApprovedDeliveryManager(agentId);
        when(deliveryAgentRepository.findByAgentTypeAndHubIdAndDeletedAtIsNullOrderByDeliveryOrderAsc(
            AgentType.COMPANY_DELIVERY, null)).thenReturn(List.of());
        CreateDeliveryAgentCommand command = CreateDeliveryAgentCommand.builder()
            .agentId(agentId)
            .agentType(AgentType.COMPANY_DELIVERY)
            .hubId(null)
            .build();

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryAgentCommandService.create(command, UserRole.MASTER, null));

        assertEquals(ErrorCode.HUB_ID_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("같은 (담당자 유형, 허브) 그룹의 정원은 최대 10명이다")
    void rejectsAgentCapacityExceeded() {
        UUID agentId = UUID.randomUUID();
        stubApprovedDeliveryManager(agentId);
        List<DeliveryAgent> full = List.of(
            DeliveryAgent.builder().agentId(UUID.randomUUID()).agentType(AgentType.HUB_DELIVERY)
                .deliveryOrder(0).build(),
            DeliveryAgent.builder().agentId(UUID.randomUUID()).agentType(AgentType.HUB_DELIVERY)
                .deliveryOrder(1).build(),
            DeliveryAgent.builder().agentId(UUID.randomUUID()).agentType(AgentType.HUB_DELIVERY)
                .deliveryOrder(2).build(),
            DeliveryAgent.builder().agentId(UUID.randomUUID()).agentType(AgentType.HUB_DELIVERY)
                .deliveryOrder(3).build(),
            DeliveryAgent.builder().agentId(UUID.randomUUID()).agentType(AgentType.HUB_DELIVERY)
                .deliveryOrder(4).build(),
            DeliveryAgent.builder().agentId(UUID.randomUUID()).agentType(AgentType.HUB_DELIVERY)
                .deliveryOrder(5).build(),
            DeliveryAgent.builder().agentId(UUID.randomUUID()).agentType(AgentType.HUB_DELIVERY)
                .deliveryOrder(6).build(),
            DeliveryAgent.builder().agentId(UUID.randomUUID()).agentType(AgentType.HUB_DELIVERY)
                .deliveryOrder(7).build(),
            DeliveryAgent.builder().agentId(UUID.randomUUID()).agentType(AgentType.HUB_DELIVERY)
                .deliveryOrder(8).build(),
            DeliveryAgent.builder().agentId(UUID.randomUUID()).agentType(AgentType.HUB_DELIVERY)
                .deliveryOrder(9).build()
        );
        when(deliveryAgentRepository.findByAgentTypeAndHubIdAndDeletedAtIsNullOrderByDeliveryOrderAsc(
            AgentType.HUB_DELIVERY, null)).thenReturn(full);
        CreateDeliveryAgentCommand command = CreateDeliveryAgentCommand.builder()
            .agentId(agentId)
            .agentType(AgentType.HUB_DELIVERY)
            .build();

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryAgentCommandService.create(command, UserRole.MASTER, null));

        assertEquals(ErrorCode.DELIVERY_AGENT_LIMIT_EXCEEDED, exception.getErrorCode());
    }

    @Test
    @DisplayName("배송담당자(DELIVERY_MANAGER) 역할이 아닌 사용자는 배송담당자로 등록할 수 없다")
    void rejectsWhenAgentUserHasWrongRole() {
        UUID agentId = UUID.randomUUID();
        when(userServiceClient.getUser(agentId)).thenReturn(
            new UserServiceUserResponseDto(agentId, UserRole.HUB_MANAGER, UserStatus.APPROVED));
        CreateDeliveryAgentCommand command = CreateDeliveryAgentCommand.builder()
            .agentId(agentId)
            .agentType(AgentType.HUB_DELIVERY)
            .build();

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryAgentCommandService.create(command, UserRole.MASTER, null));

        assertEquals(ErrorCode.DELIVERY_AGENT_INVALID_USER_ROLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("승인(APPROVED)되지 않은 사용자는 배송담당자로 등록할 수 없다")
    void rejectsWhenAgentUserNotApproved() {
        UUID agentId = UUID.randomUUID();
        when(userServiceClient.getUser(agentId)).thenReturn(
            new UserServiceUserResponseDto(agentId, UserRole.DELIVERY_MANAGER, UserStatus.PENDING));
        CreateDeliveryAgentCommand command = CreateDeliveryAgentCommand.builder()
            .agentId(agentId)
            .agentType(AgentType.HUB_DELIVERY)
            .build();

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryAgentCommandService.create(command, UserRole.MASTER, null));

        assertEquals(ErrorCode.DELIVERY_AGENT_USER_NOT_APPROVED, exception.getErrorCode());
    }

    @Test
    @DisplayName("유효성 검증을 모두 통과하면 순번 0으로 배송담당자가 생성된다")
    void createsAgentSuccessfully() {
        UUID agentId = UUID.randomUUID();
        stubApprovedDeliveryManager(agentId);
        when(deliveryAgentRepository.findByAgentTypeAndHubIdAndDeletedAtIsNullOrderByDeliveryOrderAsc(
            AgentType.HUB_DELIVERY, null)).thenReturn(List.of());
        when(deliveryAgentRepository.save(any(DeliveryAgent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateDeliveryAgentCommand command = CreateDeliveryAgentCommand.builder()
            .agentId(agentId)
            .agentType(AgentType.HUB_DELIVERY)
            .build();

        CreateDeliveryAgentResponseDto result = deliveryAgentCommandService.create(command, UserRole.MASTER, null);

        assertEquals(agentId, result.agentId());
        assertEquals(0, result.deliveryOrder());
    }

    @Test
    @DisplayName("배송담당자 유형은 수정으로 바꿀 수 없다(삭제 후 재등록 필요)")
    void rejectsGroupChangeOnUpdateAgentType() {
        UUID agentId = UUID.randomUUID();
        DeliveryAgent existing = DeliveryAgent.builder()
            .agentId(agentId)
            .agentType(AgentType.HUB_DELIVERY)
            .deliveryOrder(0)
            .build();
        when(deliveryAgentRepository.findByIdAndDeletedAtIsNull(agentId)).thenReturn(Optional.of(existing));
        UpdateDeliveryAgentCommand command = UpdateDeliveryAgentCommand.builder()
            .agentType(AgentType.COMPANY_DELIVERY)
            .build();

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryAgentCommandService.update(agentId, command, UserRole.MASTER, null));

        assertEquals(ErrorCode.DELIVERY_AGENT_GROUP_CHANGE_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    @DisplayName("소속 허브도 수정으로 바꿀 수 없다(삭제 후 재등록 필요)")
    void rejectsGroupChangeOnUpdateHubId() {
        UUID agentId = UUID.randomUUID();
        UUID originalHub = UUID.randomUUID();
        DeliveryAgent existing = DeliveryAgent.builder()
            .agentId(agentId)
            .hubId(originalHub)
            .agentType(AgentType.COMPANY_DELIVERY)
            .deliveryOrder(0)
            .build();
        when(deliveryAgentRepository.findByIdAndDeletedAtIsNull(agentId)).thenReturn(Optional.of(existing));
        UpdateDeliveryAgentCommand command = UpdateDeliveryAgentCommand.builder()
            .hubId(UUID.randomUUID())
            .build();

        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryAgentCommandService.update(agentId, command, UserRole.MASTER, null));

        assertEquals(ErrorCode.DELIVERY_AGENT_GROUP_CHANGE_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    @DisplayName("배송 담당자 본인은 다른 배송담당자를 삭제할 권한이 없다(마스터/허브 관리자만 가능)")
    void blocksNonManagerRoleFromDeleting() {
        BusinessException exception = assertThrows(BusinessException.class,
            () -> deliveryAgentCommandService.delete(
                UUID.randomUUID(), UUID.randomUUID(), UserRole.DELIVERY_MANAGER, null));

        assertEquals(ErrorCode.DELIVERY_AGENT_FORBIDDEN, exception.getErrorCode());
    }
}