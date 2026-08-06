package com.logistics.hub.hub.controller;

import com.logistics.hub.hub.dto.HubCreateRequestDto;
import com.logistics.hub.hub.dto.HubDeleteResponseDto;
import com.logistics.hub.hub.dto.HubPageResponseDto;
import com.logistics.hub.hub.dto.HubResponseDto;
import com.logistics.hub.hub.dto.HubUpdateRequestDto;
import com.logistics.hub.hub.service.HubService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/hubs")
@RequiredArgsConstructor
public class HubController {

    private final HubService hubService;

    @PostMapping
    public ResponseEntity<HubResponseDto> createHub(@Valid @RequestBody HubCreateRequestDto request) {
        HubResponseDto response = hubService.createHub(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{hubId}")
    public ResponseEntity<HubResponseDto> getHub(@PathVariable UUID hubId) {
        HubResponseDto response = hubService.getHub(hubId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{hubId}")
    public ResponseEntity<HubResponseDto> updateHub(
        @PathVariable UUID hubId,
        @Valid @RequestBody HubUpdateRequestDto request
        ) {
        HubResponseDto response = hubService.updateHub(hubId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{hubId}")
    public ResponseEntity<HubDeleteResponseDto> deletedHub(@PathVariable UUID hubId) {
        HubDeleteResponseDto response = hubService.deleteHub(hubId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<HubPageResponseDto> searchHubs(
        @RequestParam(required = false) String keyword,
        @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        HubPageResponseDto response = hubService.searchHubs(keyword, pageable);
        return ResponseEntity.ok(response);
    }
}
