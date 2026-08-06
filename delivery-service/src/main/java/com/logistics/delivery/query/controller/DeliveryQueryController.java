package com.logistics.delivery.query.controller;

import com.logistics.delivery.global.common.UserRole;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deliveries")
public class DeliveryQueryController {

    private final DeliveryQueryService deliveryQueryService;

    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryDetailResponseDto> getDelivery(
            @RequestHeader("X-User-Role") UserRole userRole,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @RequestHeader(value = "X-Company-Id", required = false) UUID requesterCompanyId,
            @PathVariable UUID deliveryId
    ) {
        DeliveryDetailResponseDto result = deliveryQueryService.getDelivery(
            userRole, requesterId, requesterHubId, requesterCompanyId, deliveryId);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<DeliveryResponseDto> searchDelivery(
        @RequestHeader("X-User-Role") UserRole userRole,
        @RequestHeader("X-User-Id") UUID requesterId,
        @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
        @RequestHeader(value = "X-Company-Id", required = false) UUID requesterCompanyId,
        @ModelAttribute @Valid DeliverySearchRequestDto request,
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
        ) {
        DeliveryResponseDto result = deliveryQueryService.searchDelivery(
            userRole, requesterId, requesterHubId, requesterCompanyId, request, pageable);
        return ResponseEntity.ok(result);
    }

}
