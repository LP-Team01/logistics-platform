package com.logistics.user.user.dto.request;

import com.logistics.user.user.entity.UserRole;
import com.logistics.user.user.entity.UserStatus;

import java.util.UUID;

public record UserSearchCondition(
    UserRole role,
    UserStatus status,
    UUID hubId,
    UUID companyId,
    String username
) {
}
