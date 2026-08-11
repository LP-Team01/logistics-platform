package com.logistics.delivery.query.controller;

import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.query.application.CompanyRouteQueryService;
import com.logistics.delivery.query.dto.response.CompanyRouteResponseDto;
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
@Tag(name = "업체 배송 경로", description = "목적지 허브에서 수령 업체까지의 배송 경로 기록 조회 API")
public class CompanyRouteQueryController {

    private final CompanyRouteQueryService companyRouteQueryService;

    @GetMapping("/{deliveryId}/company-route-records")
    @Operation(
        summary = "업체 배송 경로 조회",
        description = "배송 식별자로 목적지 허브에서 수령 업체까지의 배송 경로 기록을 조회합니다."
    )
    public ResponseEntity<CompanyRouteResponseDto> getCompanyRouteRecords(
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
        CompanyRouteResponseDto result = companyRouteQueryService.getCompanyRouteRecords(
            userRole, requesterId, requesterHubId, requesterCompanyId, deliveryId);
        return ResponseEntity.ok(result);
    }
}
