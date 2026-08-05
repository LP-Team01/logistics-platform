package com.logistics.delivery.query.controller;

import com.logistics.delivery.query.application.DeliveryRouteQueryService;
import com.logistics.delivery.query.dto.response.DeliveryRouteDetailResponseDto;
import com.logistics.delivery.query.dto.response.DeliveryRouteResponseDto;
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
public class DeliveryRouteQueryController {

    private final DeliveryRouteQueryService deliveryRouteQueryService;

    @GetMapping("/{deliveryId}/route-records")
    public ResponseEntity<DeliveryRouteResponseDto> getRouteRecords(
            @PathVariable UUID deliveryId
    ) {
        DeliveryRouteResponseDto result = deliveryRouteQueryService.getRouteRecords(deliveryId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{deliveryId}/route-records/{sequence}")
    public ResponseEntity<DeliveryRouteDetailResponseDto> getRouteRecord(
        @PathVariable UUID deliveryId,
        @PathVariable Integer sequence
    ) {
        DeliveryRouteDetailResponseDto result = deliveryRouteQueryService.getRouteRecord(deliveryId, sequence);
        return ResponseEntity.ok(result);
    }

}
