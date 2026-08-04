package com.logistics.user.dto.response;

import com.logistics.user.core.User;
import com.logistics.user.core.UserRole;
import com.logistics.user.core.UserStatus;

import java.util.UUID;

public record UserResponseDto(
    UUID userId,
    String username,
    UserRole role,
    UserStatus status,
    UUID hubId,
    UUID companyId
) {

    public static UserResponseDto from(User user) {
        return new UserResponseDto(
            user.getUserId(),
            user.getUsername(),
            user.getRole(),
            user.getStatus(),
            user.getHubId(),
            user.getCompanyId()
        );
    }
}
