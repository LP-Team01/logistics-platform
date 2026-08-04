package com.logistics.user.dto.request;

import com.logistics.user.core.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UserRequestDto(
    @NotBlank(message = "아이디는 필수입니다.")
    @Pattern(
        regexp = "^[a-z0-9]+$",
        message = "아이디는 4~10자의 소문자(a~z)와 숫자(0~9)로만 구성되어야 합니다."
    )
    @Size(min = 4, max = 10)
    String username,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(// 대문자 + 소문자 + 숫자 + 특수문자 각 1개 이상, 공백 불가, 8~15자
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*#?&])(?=\\S+$).{8,15}$",
        message = "비밀번호는 8~15자이며 대문자·소문자·숫자·특수문자(@$!%*#?&)를 각각 1개 이상 포함해야 합니다."
    )
    @Size(min = 8, max = 15)
    String password,

    String slackId,
    UserRole role,
    UUID hubId,
    UUID companyId
) {
}
