package com.logistics.delivery.query.controller;

import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.query.application.DeliveryRouteQueryService;
import com.logistics.delivery.query.dto.response.DeliveryRouteDetailResponseDto;
import com.logistics.delivery.query.dto.response.DeliveryRouteResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deliveries")
@Tag(name = "배송 경로 기록", description = "허브 간 구간별 배송 경로 기록 조회 API")
public class DeliveryRouteQueryController {

    private final DeliveryRouteQueryService deliveryRouteQueryService;

    @GetMapping("/{deliveryId}/route-records")
    @Operation(
        summary = "허브 경로 기록 목록 조회",
        description = "배송 식별자로 허브 간 전체 구간의 경로 기록 목록을 순번(sequence) 순으로 조회합니다."
    )
    public ResponseEntity<DeliveryRouteResponseDto> getRouteRecords(
            @Parameter(description = "요청자 역할", required = true)
            @RequestHeader("X-User-Role") UserRole userRole,
            @Parameter(description = "요청자 사용자 id", required = true)
            @RequestHeader("X-User-Id") UUID requesterId,
            @Parameter(description = "요청자 소속 허브 id")
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @Parameter(description = "요청자 소속 업체 id")
            @RequestHeader(value = "X-Company-Id", required = false) UUID requesterCompanyId,
            @Parameter(description = "배송 id", required = true)
            @PathVariable UUID deliveryId
    ) {
        DeliveryRouteResponseDto result = deliveryRouteQueryService.getRouteRecords(
            userRole, requesterId, requesterHubId, requesterCompanyId, deliveryId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{deliveryId}/route-records/{sequence}")
    @Operation(
        summary = "허브 경로 기록 단건 조회",
        description = "배송 식별자와 구간 순번으로 경로 기록 상세 정보를 조회합니다."
    )
    public ResponseEntity<DeliveryRouteDetailResponseDto> getRouteRecord(
        @Parameter(description = "요청자 역할", required = true)
        @RequestHeader("X-User-Role") UserRole userRole,
        @Parameter(description = "요청자 사용자 id", required = true)
        @RequestHeader("X-User-Id") UUID requesterId,
        @Parameter(description = "요청자 소속 허브 id")
        @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
        @Parameter(description = "요청자 소속 업체 id")
        @RequestHeader(value = "X-Company-Id", required = false) UUID requesterCompanyId,
        @Parameter(description = "배송 id", required = true)
        @PathVariable UUID deliveryId,
        @Parameter(description = "경로 구간 순번", required = true)
        @PathVariable Integer sequence
    ) {
        DeliveryRouteDetailResponseDto result = deliveryRouteQueryService.getRouteRecord(
            userRole, requesterId, requesterHubId, requesterCompanyId, deliveryId, sequence);
        return ResponseEntity.ok(result);
    }

}
