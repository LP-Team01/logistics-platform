package com.logistics.hub.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HubCreateRequestDto(

    @NotBlank(message = "허브명은 필수입니다.")
    String name,

    @NotBlank(message = "주소는 필수입니다.")
    String address,

    @NotNull(message = "위도는 필수입니다.")
    Double latitude,

    @NotNull(message = "경도는 필수입니다.")
    Double longitube
) {
}
