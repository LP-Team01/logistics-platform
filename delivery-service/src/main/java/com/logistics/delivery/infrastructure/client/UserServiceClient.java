package com.logistics.delivery.infrastructure.client;

import com.logistics.delivery.infrastructure.client.dto.UserServiceUserResponseDto;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    UserServiceUserResponseDto getUser(@PathVariable("userId") UUID userId);
}
