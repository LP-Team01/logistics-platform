package com.logistics.user.user.dto.response;

import com.logistics.user.user.entity.User;
import com.logistics.user.user.entity.UserRole;
import com.logistics.user.user.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserStatusResponse(
    UUID userId,
    UserStatus status,
    Instant completedAt,
    UUID completedBy
) {
    public static UserStatusResponse from(User user){
        return new UserStatusResponse(
            user.getUserId(),
            user.getStatus(),
            user.getUpdatedAt(),
            user.getUpdatedBy()
        );
    }
}
