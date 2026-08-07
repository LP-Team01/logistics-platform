package com.logistics.delivery.command.controller;

import com.logistics.delivery.command.application.CompanyRouteRecordCommandService;
import com.logistics.delivery.command.dto.command.UpdateCompanyRouteRecordCommand;
import com.logistics.delivery.command.dto.command.UpdateCompanyRouteRecordPlanCommand;
import com.logistics.delivery.command.dto.request.UpdateCompanyRouteRecordPlanRequestDto;
import com.logistics.delivery.command.dto.request.UpdateCompanyRouteRecordRequestDto;
import com.logistics.delivery.command.dto.response.UpdateCompanyRouteRecordPlanResponseDto;
import com.logistics.delivery.command.dto.response.UpdateCompanyRouteRecordResponseDto;
import com.logistics.delivery.global.common.UserRole;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deliveries")
public class CompanyRouteRecordCommandController {
    private final CompanyRouteRecordCommandService companyRouteRecordCommandService;

    @PatchMapping("/{deliveryId}/company-route-records/{recordId}/status")
    public ResponseEntity<UpdateCompanyRouteRecordResponseDto> updateStatus(
        @RequestHeader("X-User-Role") UserRole userRole,
        @RequestHeader("X-User-Id") UUID requesterId,
        @PathVariable UUID deliveryId,
        @PathVariable UUID recordId,
        @RequestBody @Valid UpdateCompanyRouteRecordRequestDto request
        ) {
        UpdateCompanyRouteRecordCommand command = request.toCommand();
        UpdateCompanyRouteRecordResponseDto result = companyRouteRecordCommandService.updateStatus(deliveryId,
            recordId, command, userRole, requesterId);
        return ResponseEntity.ok(result);
    }

    // Geocoding/Directions/방문순서 자동 계산 실패 시 수동 보정
    @PatchMapping("/{deliveryId}/company-route-records/{recordId}/route-plan")
    public ResponseEntity<UpdateCompanyRouteRecordPlanResponseDto> updateRoutePlan(
        @RequestHeader("X-User-Role") UserRole userRole,
        @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
        @PathVariable UUID deliveryId,
        @PathVariable UUID recordId,
        @RequestBody UpdateCompanyRouteRecordPlanRequestDto request
        ) {
        UpdateCompanyRouteRecordPlanCommand command = request.toCommand();
        UpdateCompanyRouteRecordPlanResponseDto result = companyRouteRecordCommandService.updateRoutePlan(deliveryId,
            recordId, command, userRole, requesterHubId);
        return ResponseEntity.ok(result);
    }
}
