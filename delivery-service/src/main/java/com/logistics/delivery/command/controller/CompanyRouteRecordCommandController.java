package com.logistics.delivery.command.controller;

import com.logistics.delivery.command.application.CompanyRouteRecordCommandService;
import com.logistics.delivery.command.dto.command.UpdateCompanyRouteRecordCommand;
import com.logistics.delivery.command.dto.request.UpdateCompanyRouteRecordRequestDto;
import com.logistics.delivery.command.dto.response.UpdateCompanyRouteRecordResponseDto;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deliveries")
public class CompanyRouteRecordCommandController {
    private final CompanyRouteRecordCommandService companyRouteRecordCommandService;

    @PatchMapping("/{deliveryId}/company-route-records/{recordId}/status")
    public ResponseEntity<UpdateCompanyRouteRecordResponseDto> updateStatus(
        @PathVariable UUID deliveryId,
        @PathVariable UUID recordId,
        @RequestBody @Valid UpdateCompanyRouteRecordRequestDto request
        ) {
        UpdateCompanyRouteRecordCommand command = request.toCommand();
        UpdateCompanyRouteRecordResponseDto result = companyRouteRecordCommandService.updateStatus(deliveryId,
            recordId, command);
        return ResponseEntity.ok(result);
    }
}
