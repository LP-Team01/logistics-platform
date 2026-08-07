package com.logistics.delivery.query.controller;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.global.common.UserRole;
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
        @RequestHeader("X-User-Role") UserRole userRole,
        @RequestHeader("X-User-Id") UUID requesterId,
        @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
        @ModelAttribute @Valid DeliveryAgentSearchRequestDto request,
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
        ) {
        DeliveryAgentResponseDto result = deliveryAgentQueryService.searchDeliveryAgents(
            userRole, requesterId, requesterHubId, request, pageable);
        return ResponseEntity.ok(result);
    }

    // "매일 아침 6시 발송" 트리거 담당 서비스가 조회할 당일 방문 계획
    @GetMapping("/{agentId}/today-route")
    public ResponseEntity<TodayRouteResponseDto> getTodayRoute(
            @RequestHeader("X-User-Role") UserRole userRole,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @PathVariable UUID agentId
    ) {
        TodayRouteResponseDto result = deliveryAgentQueryService.getTodayRoute(
            userRole, requesterId, requesterHubId, agentId);
        return ResponseEntity.ok(result);
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
