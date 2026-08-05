package com.logistics.delivery.query.controller;

import com.logistics.delivery.query.application.DeliveryQueryService;
import com.logistics.delivery.query.dto.reponse.DeliveryDetailResponseDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deliveries")
public class DeliveryQueryController {

    private final DeliveryQueryService deliveryQueryService;

    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryDetailResponseDto> getDelivery(@PathVariable UUID deliveryId) {
        DeliveryDetailResponseDto result = deliveryQueryService.getDelivery(deliveryId);
        return ResponseEntity.ok(result);
    }

}
