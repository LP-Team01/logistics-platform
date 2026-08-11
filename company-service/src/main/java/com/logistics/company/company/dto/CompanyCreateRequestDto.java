package com.logistics.company.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CompanyCreateRequestDto(
    @NotNull(message = "관리 허브 ID는 필수입니다.")
    UUID hubId,

    @NotBlank(message = "업체명은 필수입니다.")
    String name,

    @NotBlank(message = "업체 타입은 필수입니다.")
    String type,

    @NotBlank(message = "업체 주소는 필수입니다.")
    String address
) {}
