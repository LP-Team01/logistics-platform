package com.logistics.user.user.dto.response;

import com.logistics.user.user.entity.User;
import com.logistics.user.user.entity.UserRole;
import com.logistics.user.user.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;
// (회원가입/단건조회/목록조회/로그인 공용)
public record UserResponseDto(
    UUID userId,
    String username,
    String slackId,
    UserRole role,
    UserStatus status,
    UUID hubId,
    UUID companyId,
    Instant createdAt
) {

    public static UserResponseDto from(User user) {
        return new UserResponseDto(
            user.getUserId(),
            user.getUsername(),
            user.getSlackId(),
            user.getRole(),
            user.getStatus(),
            user.getHubId(),
            user.getCompanyId(),
            user.getCreatedAt()
        );
    }
}
