package com.logistics.delivery.query.controller;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.global.common.InternalServiceValidator;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import com.logistics.delivery.query.application.DeliveryAgentQueryService;
import com.logistics.delivery.query.dto.response.DeliveryAgentDetailResponseDto;
import com.logistics.delivery.query.dto.response.DeliveryAgentResponseDto;
import com.logistics.delivery.query.dto.response.TodayRouteResponseDto;
import com.logistics.delivery.query.dto.request.DeliveryAgentSearchRequestDto;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery-agents")
public class DeliveryAgentQueryController {

    private final DeliveryAgentQueryService deliveryAgentQueryService;
    private final InternalServiceValidator internalServiceValidator;

    @GetMapping("/{agentId}")
    public ResponseEntity<DeliveryAgentDetailResponseDto> getDeliveryAgent(
            @RequestHeader("X-User-Role") UserRole userRole,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @PathVariable UUID agentId
    ) {
        DeliveryAgentDetailResponseDto result = deliveryAgentQueryService.getDeliveryAgent(
            userRole, requesterId, requesterHubId, agentId);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<DeliveryAgentResponseDto> searchDeliveryAgents(
        @RequestHeader(value = "X-User-Role", required = false) UserRole userRole,
        @RequestHeader(value = "X-User-Id", required = false) UUID requesterId,
        @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
        @RequestHeader(value = "X-Internal-Service", required = false) String serviceName,
        @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey,
        @ModelAttribute @Valid DeliveryAgentSearchRequestDto request,
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
        ) {
        UserRole effectiveRole = resolveRole(serviceName, serviceKey, userRole, requesterId);
        DeliveryAgentResponseDto result = deliveryAgentQueryService.searchDeliveryAgents(
            effectiveRole, requesterId, requesterHubId, request, pageable);
        return ResponseEntity.ok(result);
    }

    // "매일 아침 6시 발송" 트리거 담당 서비스(ai-notification-service)가 조회할 당일 방문 계획 - 내부 서비스 호출 허용
    @GetMapping("/{agentId}/today-route")
    public ResponseEntity<TodayRouteResponseDto> getTodayRoute(
            @RequestHeader(value = "X-User-Role", required = false) UserRole userRole,
            @RequestHeader(value = "X-User-Id", required = false) UUID requesterId,
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @RequestHeader(value = "X-Internal-Service", required = false) String serviceName,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey,
            @PathVariable UUID agentId
    ) {
        UserRole effectiveRole = resolveRole(serviceName, serviceKey, userRole, requesterId);
        TodayRouteResponseDto result = deliveryAgentQueryService.getTodayRoute(
            effectiveRole, requesterId, requesterHubId, agentId);
        return ResponseEntity.ok(result);
    }

    // X-Internal-Service(-Key)가 유효하면 내부 서비스 호출로 보고 전체 조회 권한(MASTER)을 부여, 없으면 기존 사용자 헤더를 요구
    private UserRole resolveRole(String serviceName, String serviceKey, UserRole userRole, UUID requesterId) {
        if (serviceName != null || serviceKey != null) {
            internalServiceValidator.validateInternalService(serviceName, serviceKey);
            return UserRole.MASTER;
        }
        if (userRole == null || requesterId == null) {
            throw new BusinessException(ErrorCode.DELIVERY_AGENT_QUERY_FORBIDDEN);
        }
        return userRole;
    }

    @GetMapping("/next")
    public ResponseEntity<DeliveryAgentDetailResponseDto> getNextDeliveryAgent(
        @RequestParam AgentType agentType,
        @RequestParam(required = false) UUID hubId
        ) {
        DeliveryAgentDetailResponseDto result = deliveryAgentQueryService.getNextDeliveryAgent(agentType, hubId);
        return ResponseEntity.ok(result);
    }

}
