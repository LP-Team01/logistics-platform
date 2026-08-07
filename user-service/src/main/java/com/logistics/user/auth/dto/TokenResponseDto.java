package com.logistics.user.auth.dto;

public record TokenResponseDto(
    String accessToken,
    String tokenType,
    int expiresIn
) {
}
