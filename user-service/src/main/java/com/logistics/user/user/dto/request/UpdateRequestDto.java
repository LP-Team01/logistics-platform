package com.logistics.user.user.dto.request;

import com.logistics.user.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateRequestDto(
    @Pattern(
        regexp = "^[a-z0-9]+$",
        message = "아이디는 4~10자의 소문자(a~z)와 숫자(0~9)로만 구성되어야 합니다."
    )
    @Size(min = 4, max = 10)
    String username,

    String slackId,
    UserRole role,
    UUID hubId,
    UUID companyId
) {
}
