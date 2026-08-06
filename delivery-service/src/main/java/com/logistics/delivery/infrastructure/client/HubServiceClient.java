package com.logistics.delivery.infrastructure.client;

import com.logistics.delivery.infrastructure.client.dto.HubServiceHubResponseDto;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "hub-service")
public interface HubServiceClient {

    @GetMapping("/api/hubs/{hubId}")
    HubServiceHubResponseDto getHub(@PathVariable("hubId") UUID hubId);
}