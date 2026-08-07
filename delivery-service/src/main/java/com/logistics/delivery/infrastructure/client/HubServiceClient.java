package com.logistics.delivery.infrastructure.client;

import com.logistics.delivery.infrastructure.client.dto.HubServiceHubResponseDto;
import com.logistics.delivery.infrastructure.client.dto.HubServiceRoutePathResponseDto;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "hub-service")
public interface HubServiceClient {

    @GetMapping("/api/hubs/{hubId}")
    HubServiceHubResponseDto getHub(@PathVariable("hubId") UUID hubId);

    // 내부 전용 API
    @GetMapping("/api/hub-routes/path")
    HubServiceRoutePathResponseDto getRoutePath(
        @RequestHeader("X-Internal-Service") String serviceName,
        @RequestHeader("X-Internal-Service-Key") String serviceKey,
        @RequestParam("departureHubId") UUID departureHubId,
        @RequestParam("arrivalHubId") UUID arrivalHubId
    );
}
