package com.logistics.delivery.query.controller;

import com.logistics.delivery.query.application.DeliveryQueryService;
import com.logistics.delivery.query.dto.response.DeliveryDetailResponseDto;
import com.logistics.delivery.query.dto.response.DeliveryResponseDto;
import com.logistics.delivery.query.dto.request.DeliverySearchRequestDto;
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

    @GetMapping
    public ResponseEntity<DeliveryResponseDto> searchDelivery(
        @ModelAttribute @Valid DeliverySearchRequestDto request,
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
        ) {
        DeliveryResponseDto result = deliveryQueryService.searchDelivery(request, pageable);
        return ResponseEntity.ok(result);
    }

}
