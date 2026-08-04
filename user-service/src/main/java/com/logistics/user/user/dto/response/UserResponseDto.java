package com.logistics.user.user.dto.response;

import com.logistics.user.user.entity.User;
import com.logistics.user.user.entity.UserRole;
import com.logistics.user.user.entity.UserStatus;

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
