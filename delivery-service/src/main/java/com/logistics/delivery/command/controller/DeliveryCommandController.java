package com.logistics.delivery.command.controller;

import com.logistics.delivery.command.application.DeliveryCommandService;
import com.logistics.delivery.command.dto.command.CreateDeliveryCommand;
import com.logistics.delivery.command.dto.request.CreateDeliveryRequestDto;
import com.logistics.delivery.command.dto.response.CreateDeliveryResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deliveries")
public class DeliveryCommandController {
    private final DeliveryCommandService deliveryCommandService;

    @PostMapping
    public ResponseEntity<CreateDeliveryResponseDto> create(@RequestBody @Valid CreateDeliveryRequestDto request) {
        //TODO: HubService 만들어진 후 hubClient로 hub유효성 검사
        CreateDeliveryCommand command = request.toCommand();
        CreateDeliveryResponseDto result = deliveryCommandService.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
