package com.logistics.delivery.command.controller;

import com.logistics.delivery.command.application.DeliverRouteRecordCommandService;
import com.logistics.delivery.command.dto.command.UpdateDeliveryRouteRecordCommand;
import com.logistics.delivery.command.dto.request.UpdateDeliveryRouteRecordRequestDto;
import com.logistics.delivery.command.dto.response.UpdateDeliveryRouteRecordResponseDto;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deliveries")
public class DeliveryRouteRecordCommandController {
    private final DeliverRouteRecordCommandService deliverRouteRecordCommandService;

    @PatchMapping("/{deliveryId}/route-records/{sequence}/status")
    public ResponseEntity<UpdateDeliveryRouteRecordResponseDto> updateStatus(
        @PathVariable UUID deliveryId,
        @PathVariable Integer sequence,
        @RequestBody @Valid UpdateDeliveryRouteRecordRequestDto request
        ) {
        UpdateDeliveryRouteRecordCommand command = request.toCommand();
        UpdateDeliveryRouteRecordResponseDto result = deliverRouteRecordCommandService.updateStatus(deliveryId,
            sequence, command);
        return ResponseEntity.ok(result);
    }
}
