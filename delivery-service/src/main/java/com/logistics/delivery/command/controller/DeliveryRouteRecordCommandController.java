package com.logistics.delivery.command.controller;

import com.logistics.delivery.command.application.DeliveryRouteRecordCommandService;
import com.logistics.delivery.command.dto.command.UpdateDeliveryRouteRecordCommand;
import com.logistics.delivery.command.dto.request.UpdateDeliveryRouteRecordRequestDto;
import com.logistics.delivery.command.dto.response.UpdateDeliveryRouteRecordResponseDto;
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
@Tag(name = "배송 경로 기록", description = "허브 간 구간별 배송 경로 기록 상태 변경 API")
public class DeliveryRouteRecordCommandController {
    private final DeliveryRouteRecordCommandService deliverRouteRecordCommandService;

    @PatchMapping("/{deliveryId}/route-records/{sequence}/status")
    @Operation(
        summary = "허브 경로 기록 상태 변경",
        description = "배송의 특정 구간(sequence) 상태를 변경합니다. "
            + "ARRIVED/COMPLETED로 전이할 때는 actualDistance/actualDuration이 필수이며, "
            + "본인이 담당한 경로인지(agentId == 요청자 X-User-Id, DELIVERY 역할일 때) 검증합니다."
    )
    public ResponseEntity<UpdateDeliveryRouteRecordResponseDto> updateStatus(
        @Parameter(description = "요청자 역할", required = true)
        @RequestHeader("X-User-Role") UserRole userRole,
        @Parameter(description = "요청자 사용자 id", required = true)
        @RequestHeader("X-User-Id") UUID requesterId,
        @Parameter(description = "요청자 소속 허브 id")
        @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
        @Parameter(description = "배송 id", required = true)
        @PathVariable UUID deliveryId,
        @Parameter(description = "경로 구간 순번", required = true)
        @PathVariable Integer sequence,
        @RequestBody @Valid UpdateDeliveryRouteRecordRequestDto request
        ) {
        UpdateDeliveryRouteRecordCommand command = request.toCommand();
        UpdateDeliveryRouteRecordResponseDto result = deliverRouteRecordCommandService.updateStatus(deliveryId,
            sequence, command, userRole, requesterId, requesterHubId);
        return ResponseEntity.ok(result);
    }
}
