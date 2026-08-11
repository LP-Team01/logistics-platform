package com.logistics.delivery.infrastructure.client;

import com.logistics.delivery.infrastructure.client.dto.HubServiceHubResponseDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HubQueryService {

    private final HubServiceClient hubServiceClient;

    @Cacheable(value = "hubs", key = "#hubId")
    public HubServiceHubResponseDto getHub(UUID hubId) {
        return hubServiceClient.getHub(hubId);
    }
}
