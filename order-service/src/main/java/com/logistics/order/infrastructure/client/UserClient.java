package com.logistics.order.infrastructure.client;

import com.logistics.order.infrastructure.client.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * User Service 사용자 조회
 */
@FeignClient(
        name = "user-service",
        path = "/api/users"
)
public interface UserClient {

    /**
     * 사용자 이름과 Slack ID 조회
     */
    @GetMapping("/{userId}")
    UserResponse getUser(
            @PathVariable UUID userId
    );
}
