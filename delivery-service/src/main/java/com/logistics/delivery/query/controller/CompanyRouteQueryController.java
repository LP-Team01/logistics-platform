package com.logistics.delivery.query.controller;

import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.query.application.CompanyRouteQueryService;
import com.logistics.delivery.query.dto.response.CompanyRouteResponseDto;
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
public class CompanyRouteQueryController {

    private final CompanyRouteQueryService companyRouteQueryService;

    @GetMapping("/{deliveryId}/company-route-records")
    public ResponseEntity<CompanyRouteResponseDto> getCompanyRouteRecords(
            @RequestHeader("X-User-Role") UserRole userRole,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @RequestHeader(value = "X-Company-Id", required = false) UUID requesterCompanyId,
            @PathVariable UUID deliveryId
    ) {
        CompanyRouteResponseDto result = companyRouteQueryService.getCompanyRouteRecords(
            userRole, requesterId, requesterHubId, requesterCompanyId, deliveryId);
        return ResponseEntity.ok(result);
    }
}
