package com.logistics.delivery.infrastructure.client.dto;

import com.logistics.delivery.global.common.UserRole;
import java.util.UUID;

public record UserServiceUserResponseDto(
    UUID userId,
    UserRole role
) {
}
