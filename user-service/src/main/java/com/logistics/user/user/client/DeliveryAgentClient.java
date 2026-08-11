package com.logistics.user.user.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "delivery-service")
public interface DeliveryAgentClient {

    @PostMapping("/api/delivery-agents")
    DeliveryAgentResponse createAgent(
        @RequestHeader("X-User-Id") UUID requesterId,
        @RequestHeader("X-User-Role") String userRole,
        @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
        @RequestBody CreateDeliveryAgentRequest request
    );
}
