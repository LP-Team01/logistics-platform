package com.logistics.hub.hub.controller;

import com.logistics.hub.hub.dto.HubCreateRequestDto;
import com.logistics.hub.hub.dto.HubResponseDto;
import com.logistics.hub.hub.service.HubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
