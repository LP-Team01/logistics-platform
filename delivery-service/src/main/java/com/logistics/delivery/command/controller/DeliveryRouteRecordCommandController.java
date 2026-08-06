package com.logistics.delivery.command.controller;

import com.logistics.delivery.command.application.DeliveryRouteRecordCommandService;
import com.logistics.delivery.command.dto.command.UpdateDeliveryRouteRecordCommand;
import com.logistics.delivery.command.dto.request.UpdateDeliveryRouteRecordRequestDto;
import com.logistics.delivery.command.dto.response.UpdateDeliveryRouteRecordResponseDto;
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
public class DeliveryRouteRecordCommandController {
    private final DeliveryRouteRecordCommandService deliverRouteRecordCommandService;

    @PatchMapping("/{deliveryId}/route-records/{sequence}/status")
    public ResponseEntity<UpdateDeliveryRouteRecordResponseDto> updateStatus(
        @RequestHeader("X-User-Role") UserRole userRole,
        @RequestHeader("X-User-Id") UUID requesterId,
        @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
        @PathVariable UUID deliveryId,
        @PathVariable Integer sequence,
        @RequestBody @Valid UpdateDeliveryRouteRecordRequestDto request
        ) {
        UpdateDeliveryRouteRecordCommand command = request.toCommand();
        UpdateDeliveryRouteRecordResponseDto result = deliverRouteRecordCommandService.updateStatus(deliveryId,
            sequence, command, userRole, requesterId, requesterHubId);
        return ResponseEntity.ok(result);
    }
}
