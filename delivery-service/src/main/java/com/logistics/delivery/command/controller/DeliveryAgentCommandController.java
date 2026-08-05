package com.logistics.delivery.command.controller;

import com.logistics.delivery.command.dto.command.CreateDeliveryAgentCommand;
import com.logistics.delivery.command.dto.command.UpdateDeliveryAgentCommand;
import com.logistics.delivery.command.application.DeliveryAgentCommandService;
import com.logistics.delivery.command.dto.request.CreateDeliveryAgentRequestDto;
import com.logistics.delivery.command.dto.request.UpdateDeliveryAgentRequestDto;
import com.logistics.delivery.command.dto.response.CreateDeliveryAgentResponseDto;
import com.logistics.delivery.command.dto.response.UpdateDeliveryAgentResponseDto;
import jakarta.validation.Valid;
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

    // TODO: X-User-Role 체크
    @PostMapping
    public ResponseEntity<CreateDeliveryAgentResponseDto> create(
            @RequestBody @Valid CreateDeliveryAgentRequestDto request
    ) {
        CreateDeliveryAgentCommand command = request.toCommand();
        CreateDeliveryAgentResponseDto result = deliveryAgentCommandService.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // TODO: X-User-Role 체크
    @PatchMapping("/{agentId}")
    public ResponseEntity<UpdateDeliveryAgentResponseDto> update(
            @PathVariable UUID agentId,
            @RequestBody @Valid UpdateDeliveryAgentRequestDto request
    ) {
        UpdateDeliveryAgentCommand command = request.toCommand();
        UpdateDeliveryAgentResponseDto result = deliveryAgentCommandService.update(agentId, command);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{agentId}")
    public ResponseEntity<Void> delete(
        @PathVariable UUID agentId,
        @RequestHeader("X-User-Id") UUID requesterId
    ) {
        deliveryAgentCommandService.delete(agentId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
