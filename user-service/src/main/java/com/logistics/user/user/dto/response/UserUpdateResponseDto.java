package com.logistics.user.user.dto.response;

import com.logistics.user.user.entity.User;
import com.logistics.user.user.entity.UserRole;
import com.logistics.user.user.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserUpdateResponseDto(
    UUID userId,
    String username,
    UserRole role,
    UserStatus status,
    UUID hubId,
    UUID companyId,
    Instant updatedAt
) {

    public static UserUpdateResponseDto from(User user) {
        return new UserUpdateResponseDto(
            user.getUserId(),
            user.getUsername(),
            user.getRole(),
            user.getStatus(),
            user.getHubId(),
            user.getCompanyId(),
            user.getUpdatedAt()
        );
    }
}
