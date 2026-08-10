package com.logistics.hub.hubroute.controller;

import com.logistics.hub.global.auth.InternalServiceValidator;
import com.logistics.hub.hubroute.dto.HubRoutePathResponseDto;
import com.logistics.hub.hubroute.service.HubRoutePathService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/hub-routes")
@RequiredArgsConstructor
public class HubRoutePathController {

    private final HubRoutePathService hubRoutePathService;
    private final InternalServiceValidator internalServiceValidator;

    @GetMapping("/path")
    public ResponseEntity<HubRoutePathResponseDto> getShortestPath(
        @RequestHeader("X-Internal-Service") String serviceName,
        @RequestHeader("X-Internal-Service-Key") String serviceKey,
        @RequestParam UUID departureHubId,
        @RequestParam UUID arrivalHubId,
        @RequestParam(defaultValue = "distance") String criteria
    ) {
        internalServiceValidator.validate(serviceName,serviceKey); // 다익스트라 로직 시작 전, 검증부터

        HubRoutePathService.PathResult result =
            hubRoutePathService.findShortestPath(departureHubId, arrivalHubId, criteria);

        HubRoutePathResponseDto response = HubRoutePathResponseDto.from(result);
        return ResponseEntity.ok(response);
    }
}
