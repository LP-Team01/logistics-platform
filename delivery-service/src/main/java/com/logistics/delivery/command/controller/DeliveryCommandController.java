package com.logistics.delivery.command.controller;

import com.logistics.delivery.command.application.DeliveryCommandService;
import com.logistics.delivery.command.dto.command.CreateDeliveryCommand;
import com.logistics.delivery.command.dto.command.UpdateDeliveryCommand;
import com.logistics.delivery.command.dto.request.CreateDeliveryRequestDto;
import com.logistics.delivery.command.dto.request.UpdateDeliveryRequestDto;
import com.logistics.delivery.command.dto.response.CreateDeliveryResponseDto;
import com.logistics.delivery.command.dto.response.UpdateDeliveryResponseDto;
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
@RequestMapping("/api/deliveries")
public class DeliveryCommandController {
    private final DeliveryCommandService deliveryCommandService;

    //TODO : 내부 호출 보장성 어떻게 가져갈지 논의 할 것
    @PostMapping
    public ResponseEntity<CreateDeliveryResponseDto> create(@RequestBody @Valid CreateDeliveryRequestDto request) {
        //TODO: HubService 만들어진 후 hubClient로 hub유효성 검사
        CreateDeliveryCommand command = request.toCommand();
        CreateDeliveryResponseDto result = deliveryCommandService.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PatchMapping("/{deliveryId}/status")
    public ResponseEntity<UpdateDeliveryResponseDto> update(
            @PathVariable UUID deliveryId,
            @RequestBody @Valid UpdateDeliveryRequestDto request) {
        UpdateDeliveryCommand command = request.toCommand();
        UpdateDeliveryResponseDto result = deliveryCommandService.update(deliveryId, command);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{deliveryId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID deliveryId,
            @RequestHeader("X-User-Id") UUID requesterId) {
        deliveryCommandService.delete(requesterId, deliveryId);
        return ResponseEntity.noContent().build();
    }
}
