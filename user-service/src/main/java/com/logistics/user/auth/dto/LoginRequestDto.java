package com.logistics.user.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
    @NotBlank(message = "아이디 입력은 필수 입니다.")
    String username,

    @NotBlank(message = "비밀번호 입력은 필수입니다.")
    String password
) {
}
