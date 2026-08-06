package com.logistics.delivery.query.controller;

import com.logistics.delivery.query.application.CompanyRouteQueryService;
import com.logistics.delivery.query.application.DeliveryRouteQueryService;
import com.logistics.delivery.query.dto.reponse.CompanyRouteResponseDto;
import com.logistics.delivery.query.dto.reponse.DeliveryRouteDetailResponseDto;
import com.logistics.delivery.query.dto.reponse.DeliveryRouteResponseDto;
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
public class CompanyRouteQueryController {

    private final CompanyRouteQueryService companyRouteQueryService;

    @GetMapping("/{deliveryId}/company-route-records")
    public ResponseEntity<CompanyRouteResponseDto> getCompanyRouteRecords(
            @PathVariable UUID deliveryId
    ) {
        CompanyRouteResponseDto result = companyRouteQueryService.getCompanyRouteRecords(deliveryId);
        return ResponseEntity.ok(result);
    }
}
