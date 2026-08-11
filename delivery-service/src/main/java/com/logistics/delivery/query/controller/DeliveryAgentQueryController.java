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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "배송 담당자", description = "배송 담당자 단건/목록 조회, 당일 방문 계획, 다음 순번 조회 API")
public class DeliveryAgentQueryController {

    private final DeliveryAgentQueryService deliveryAgentQueryService;
    private final InternalServiceValidator internalServiceValidator;

    @GetMapping("/{agentId}")
    @Operation(
        summary = "배송 담당자 단건 조회",
        description = "배송 담당자 식별자로 상세 정보를 조회합니다."
    )
    public ResponseEntity<DeliveryAgentDetailResponseDto> getDeliveryAgent(
            @Parameter(description = "요청자 역할", required = true)
            @RequestHeader("X-User-Role") UserRole userRole,
            @Parameter(description = "요청자 사용자 id", required = true)
            @RequestHeader("X-User-Id") UUID requesterId,
            @Parameter(description = "요청자 소속 허브 id")
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @Parameter(description = "배송 담당자 id", required = true)
            @PathVariable UUID agentId
    ) {
        DeliveryAgentDetailResponseDto result = deliveryAgentQueryService.getDeliveryAgent(
            userRole, requesterId, requesterHubId, agentId);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @Operation(
        summary = "배송 담당자 목록 조회/검색",
        description = "허브 id, 담당자 유형, 가용 여부로 배송 담당자 목록을 검색합니다. "
            + "X-Internal-Service(-Key) 헤더가 유효하면 내부 서비스 호출로 보고 전체 조회 권한을 부여합니다."
    )
    public ResponseEntity<DeliveryAgentResponseDto> searchDeliveryAgents(
        @Parameter(description = "요청자 역할")
        @RequestHeader(value = "X-User-Role", required = false) UserRole userRole,
        @Parameter(description = "요청자 사용자 id")
        @RequestHeader(value = "X-User-Id", required = false) UUID requesterId,
        @Parameter(description = "요청자 소속 허브 id")
        @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
        @Parameter(description = "호출 서비스명(내부 전용 검증)")
        @RequestHeader(value = "X-Internal-Service", required = false) String serviceName,
        @Parameter(description = "호출 서비스 인증 키(내부 전용 검증)")
        @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey,
        @ParameterObject @ModelAttribute @Valid DeliveryAgentSearchRequestDto request,
        @ParameterObject
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
        ) {
        UserRole effectiveRole = resolveRole(serviceName, serviceKey, userRole, requesterId);
        DeliveryAgentResponseDto result = deliveryAgentQueryService.searchDeliveryAgents(
            effectiveRole, requesterId, requesterHubId, request, pageable);
        return ResponseEntity.ok(result);
    }

    // "매일 아침 6시 발송" 트리거 담당 서비스(ai-notification-service)가 조회할 당일 방문 계획 - 내부 서비스 호출 허용
    @GetMapping("/{agentId}/today-route")
    @Operation(
        summary = "당일 방문 계획 조회",
        description = "업체 배송 담당자의 당일 방문 계획(경로, 총 거리/소요시간, 방문지 목록)을 조회합니다. "
            + "\"매일 아침 6시 발송\" 요약 알림을 위해 ai-notification-service가 내부 호출로 조회합니다."
    )
    public ResponseEntity<TodayRouteResponseDto> getTodayRoute(
            @Parameter(description = "요청자 역할")
            @RequestHeader(value = "X-User-Role", required = false) UserRole userRole,
            @Parameter(description = "요청자 사용자 id")
            @RequestHeader(value = "X-User-Id", required = false) UUID requesterId,
            @Parameter(description = "요청자 소속 허브 id")
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @Parameter(description = "호출 서비스명(내부 전용 검증)")
            @RequestHeader(value = "X-Internal-Service", required = false) String serviceName,
            @Parameter(description = "호출 서비스 인증 키(내부 전용 검증)")
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey,
            @Parameter(description = "배송 담당자 id", required = true)
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
    @Operation(
        summary = "다음 순번 배송 담당자 조회",
        description = "담당자 유형과 허브 id를 기준으로 가용한(isAvailable=true) 담당자 중 배정 순번이 가장 빠른 담당자를 조회하는 내부 전용 API입니다."
    )
    public ResponseEntity<DeliveryAgentDetailResponseDto> getNextDeliveryAgent(
        @Parameter(description = "호출 서비스명(내부 전용 검증)", required = true)
        @RequestHeader("X-Internal-Service") String serviceName,
        @Parameter(description = "호출 서비스 인증 키(내부 전용 검증)", required = true)
        @RequestHeader("X-Internal-Service-Key") String serviceKey,
        @Parameter(description = "배송 담당자 유형", required = true)
        @RequestParam AgentType agentType,
        @Parameter(description = "허브 id(HUB_DELIVERY 조회 시 미지정 가능)")
        @RequestParam(required = false) UUID hubId
        ) {
        internalServiceValidator.validateInternalService(serviceName, serviceKey);
        DeliveryAgentDetailResponseDto result = deliveryAgentQueryService.getNextDeliveryAgent(agentType, hubId);
        return ResponseEntity.ok(result);
    }

}
