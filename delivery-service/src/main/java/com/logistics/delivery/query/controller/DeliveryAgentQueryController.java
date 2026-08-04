package com.logistics.delivery.query.controller;

import com.logistics.delivery.query.application.DeliveryAgentQueryService;
import com.logistics.delivery.query.dto.reponse.DeliveryAgentDetailResponseDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery-agents")
public class DeliveryAgentQueryController {

    private final DeliveryAgentQueryService deliveryAgentQueryService;

    @GetMapping("/{agentId}")
    public ResponseEntity<DeliveryAgentDetailResponseDto> getDeliveryAgent(
            @PathVariable UUID agentId
    ) {
        DeliveryAgentDetailResponseDto result = deliveryAgentQueryService.getDeliveryAgent(agentId);
        return ResponseEntity.ok(result);
    }

}
