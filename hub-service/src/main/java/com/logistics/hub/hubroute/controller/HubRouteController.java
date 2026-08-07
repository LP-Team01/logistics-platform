package com.logistics.hub.hubroute.controller;

import com.logistics.hub.hubroute.dto.HubRouteCreateRequestDto;
import com.logistics.hub.hubroute.dto.HubRouteDeleteResponseDto;
import com.logistics.hub.hubroute.dto.HubRoutePageResponseDto;
import com.logistics.hub.hubroute.dto.HubRouteResponseDto;
import com.logistics.hub.hubroute.dto.HubRouteUpdateRequestDto;
import com.logistics.hub.hubroute.service.HubRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/hub-routes")
@RequiredArgsConstructor
public class HubRouteController {

    private final HubRouteService hubRouteService;

    @PostMapping
    public ResponseEntity<HubRouteResponseDto> createHubRoute(@Valid @RequestBody HubRouteCreateRequestDto request) {
        HubRouteResponseDto response = hubRouteService.createHubRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{hubRouteId}")
    public ResponseEntity<HubRouteResponseDto> getHubRoute(@PathVariable UUID hubRouteId) {
        HubRouteResponseDto response = hubRouteService.getHubRoute(hubRouteId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{hubRouteId}")
    public ResponseEntity<HubRouteResponseDto> updateHubRoute(
        @PathVariable UUID hubRouteId,
        @Valid @RequestBody HubRouteUpdateRequestDto request
    ) {
        HubRouteResponseDto response = hubRouteService.updateHubRoute(hubRouteId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{hubRouteId}")
    public ResponseEntity<HubRouteDeleteResponseDto> deleteHubRoute(
        @PathVariable UUID hubRouteId,
        @RequestHeader(value = "X-User-Id", required = false) UUID deletedBy
    ) {
        HubRouteDeleteResponseDto response = hubRouteService.deleteHubRoute(hubRouteId, deletedBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<HubRoutePageResponseDto> searchHubRoutes(
        @RequestParam(required = false) UUID departureHubId,
        @RequestParam(required = false) UUID arrivalHubId,
        @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        HubRoutePageResponseDto response = hubRouteService.searchHubRoutes(departureHubId, arrivalHubId, pageable);
        return ResponseEntity.ok(response);
    }
}
