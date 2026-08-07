package com.logistics.delivery.query.controller;

import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.query.application.DeliveryRouteQueryService;
import com.logistics.delivery.query.dto.response.DeliveryRouteDetailResponseDto;
import com.logistics.delivery.query.dto.response.DeliveryRouteResponseDto;
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
public class DeliveryRouteQueryController {

    private final DeliveryRouteQueryService deliveryRouteQueryService;

    @GetMapping("/{deliveryId}/route-records")
    public ResponseEntity<DeliveryRouteResponseDto> getRouteRecords(
            @RequestHeader("X-User-Role") UserRole userRole,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @RequestHeader(value = "X-Company-Id", required = false) UUID requesterCompanyId,
            @PathVariable UUID deliveryId
    ) {
        DeliveryRouteResponseDto result = deliveryRouteQueryService.getRouteRecords(
            userRole, requesterId, requesterHubId, requesterCompanyId, deliveryId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{deliveryId}/route-records/{sequence}")
    public ResponseEntity<DeliveryRouteDetailResponseDto> getRouteRecord(
        @RequestHeader("X-User-Role") UserRole userRole,
        @RequestHeader("X-User-Id") UUID requesterId,
        @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
        @RequestHeader(value = "X-Company-Id", required = false) UUID requesterCompanyId,
        @PathVariable UUID deliveryId,
        @PathVariable Integer sequence
    ) {
        DeliveryRouteDetailResponseDto result = deliveryRouteQueryService.getRouteRecord(
            userRole, requesterId, requesterHubId, requesterCompanyId, deliveryId, sequence);
        return ResponseEntity.ok(result);
    }

}
