package com.logistics.delivery.query.controller;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.query.application.DeliveryAgentQueryService;
import com.logistics.delivery.query.dto.response.DeliveryAgentDetailResponseDto;
import com.logistics.delivery.query.dto.response.DeliveryAgentResponseDto;
import com.logistics.delivery.query.dto.request.DeliveryAgentSearchRequestDto;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public ResponseEntity<DeliveryAgentResponseDto> searchDeliveryAgents(
        @ModelAttribute @Valid DeliveryAgentSearchRequestDto request,
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
        ) {
        DeliveryAgentResponseDto result = deliveryAgentQueryService.searchDeliveryAgents(request, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/next")
    public ResponseEntity<DeliveryAgentDetailResponseDto> getNextDeliveryAgent(
        @RequestParam AgentType agentType,
        @RequestParam(required = false) UUID hubId
        ) {
        DeliveryAgentDetailResponseDto result = deliveryAgentQueryService.getNextDeliveryAgent(agentType, hubId);
        return ResponseEntity.ok(result);
    }

}
