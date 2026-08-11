package com.logistics.order.infrastructure.client.dto;

import java.util.UUID;

/**
 * 사용자 조회 응답
 */
public record UserResponse(
        UUID userId,
        String username,
        String slackId
) {
}
