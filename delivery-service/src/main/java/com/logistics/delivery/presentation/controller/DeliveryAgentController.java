package com.logistics.delivery.presentation.controller;

import com.logistics.delivery.application.command.CreateDeliveryAgentCommand;
import com.logistics.delivery.application.service.DeliveryAgentCommandService;
import com.logistics.delivery.presentation.dto.request.CreateDeliveryAgentRequest;
import com.logistics.delivery.presentation.dto.response.DeliveryAgentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery-agents")
public class DeliveryAgentController {


    private final DeliveryAgentCommandService deliveryAgentCommandService;

    // TODO: X-User-Role 체크
    @PostMapping
    public ResponseEntity<DeliveryAgentResponse> create(
        @RequestBody @Valid CreateDeliveryAgentRequest request
    ) {
        CreateDeliveryAgentCommand command = request.toCommand();
        DeliveryAgentResponse result = deliveryAgentCommandService.create(command);
        return ResponseEntity.ok(result);
    }
}
