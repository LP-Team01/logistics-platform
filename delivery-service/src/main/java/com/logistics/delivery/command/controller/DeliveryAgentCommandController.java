package com.logistics.delivery.command.controller;

import com.logistics.delivery.command.dto.command.CreateDeliveryAgentCommand;
import com.logistics.delivery.command.dto.command.UpdateDeliveryAgentCommand;
import com.logistics.delivery.command.application.DeliveryAgentCommandService;
import com.logistics.delivery.command.dto.request.CreateDeliveryAgentRequestDto;
import com.logistics.delivery.command.dto.request.UpdateDeliveryAgentRequestDto;
import com.logistics.delivery.command.dto.response.CreateDeliveryAgentResponseDto;
import com.logistics.delivery.command.dto.response.UpdateDeliveryAgentResponseDto;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import jakarta.validation.Valid;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery-agents")
public class DeliveryAgentCommandController {

    private final DeliveryAgentCommandService deliveryAgentCommandService;

    @PostMapping
    public ResponseEntity<CreateDeliveryAgentResponseDto> create(
            @RequestHeader("X-User-Role") UserRole userRole,
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @RequestBody @Valid CreateDeliveryAgentRequestDto request
    ) {
        CreateDeliveryAgentCommand command = request.toCommand();
        CreateDeliveryAgentResponseDto result = deliveryAgentCommandService.create(command, userRole, requesterHubId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PatchMapping("/{agentId}")
    public ResponseEntity<UpdateDeliveryAgentResponseDto> update(
            @RequestHeader("X-User-Role") UserRole userRole,
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @PathVariable UUID agentId,
            @RequestBody @Valid UpdateDeliveryAgentRequestDto request
    ) {
        UpdateDeliveryAgentCommand command = request.toCommand();
        UpdateDeliveryAgentResponseDto result = deliveryAgentCommandService.update(
            agentId, command, userRole, requesterHubId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{agentId}")
    public ResponseEntity<Void> delete(
        @RequestHeader("X-User-Role") UserRole userRole,
        @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
        @PathVariable UUID agentId,
        @RequestHeader("X-User-Id") UUID requesterId
    ) {
        deliveryAgentCommandService.delete(agentId, requesterId, userRole, requesterHubId);
        return ResponseEntity.noContent().build();
    }
}
