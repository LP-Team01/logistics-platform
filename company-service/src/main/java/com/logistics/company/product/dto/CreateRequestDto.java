package com.logistics.company.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateRequestDto(

    @NotNull(message = "생산 업체 ID는 필수입니다.")
    UUID companyId,

    @NotNull(message = "관리 허브 ID는 필수입니다.")
    UUID hubId,

    @NotBlank(message = "상품명은 필수입니다.")
    String name,

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 0, message = "수량은 0 이상이어야 합니다.")
    Integer quantity,

    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    Integer price
) {}
