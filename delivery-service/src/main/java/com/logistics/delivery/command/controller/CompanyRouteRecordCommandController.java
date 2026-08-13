package com.logistics.delivery.command.controller;

import com.logistics.delivery.command.application.CompanyRouteRecordCommandService;
import com.logistics.delivery.command.dto.command.UpdateCompanyRouteRecordCommand;
import com.logistics.delivery.command.dto.command.UpdateCompanyRouteRecordPlanCommand;
import com.logistics.delivery.command.dto.request.UpdateCompanyRouteRecordPlanRequestDto;
import com.logistics.delivery.command.dto.request.UpdateCompanyRouteRecordRequestDto;
import com.logistics.delivery.command.dto.response.UpdateCompanyRouteRecordPlanResponseDto;
import com.logistics.delivery.command.dto.response.UpdateCompanyRouteRecordResponseDto;
import com.logistics.delivery.global.common.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "업체 배송 경로", description = "목적지 허브에서 수령 업체까지의 배송 경로 기록 상태/계획 변경 API")
public class CompanyRouteRecordCommandController {
    private final CompanyRouteRecordCommandService companyRouteRecordCommandService;

    @PatchMapping("/{deliveryId}/company-route-records/{recordId}/status")
    @Operation(
        summary = "업체 배송 경로 상태 변경",
        description = "업체 배송 경로 기록의 상태를 변경합니다."
    )
    public ResponseEntity<UpdateCompanyRouteRecordResponseDto> updateStatus(
        @Parameter(description = "요청자 역할", required = true)
        @RequestHeader("X-User-Role") UserRole userRole,
        @Parameter(description = "요청자 사용자 id", required = true)
        @RequestHeader("X-User-Id") UUID requesterId,
        @Parameter(description = "배송 id", required = true)
        @PathVariable UUID deliveryId,
        @Parameter(description = "업체 배송 경로 기록 id", required = true)
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
    @Operation(
        summary = "업체 배송 경로 계획 수동 보정",
        description = "네이버 Geocoding/Directions API 또는 방문 순서 자동 계산이 실패했을 때 "
            + "위경도, 예상 거리/소요시간, 방문 순서를 수동으로 보정합니다. 최소 하나의 필드는 값이 있어야 합니다."
    )
    public ResponseEntity<UpdateCompanyRouteRecordPlanResponseDto> updateRoutePlan(
        @Parameter(description = "요청자 역할", required = true)
        @RequestHeader("X-User-Role") UserRole userRole,
        @Parameter(description = "요청자 소속 허브 id")
        @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
        @Parameter(description = "배송 id", required = true)
        @PathVariable UUID deliveryId,
        @Parameter(description = "업체 배송 경로 기록 id", required = true)
        @PathVariable UUID recordId,
        @RequestBody @Valid UpdateCompanyRouteRecordPlanRequestDto request
        ) {
        UpdateCompanyRouteRecordPlanCommand command = request.toCommand();
        UpdateCompanyRouteRecordPlanResponseDto result = companyRouteRecordCommandService.updateRoutePlan(deliveryId,
            recordId, command, userRole, requesterHubId);
        return ResponseEntity.ok(result);
    }
}
