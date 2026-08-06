package com.logistics.hub.hub.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

public record HubUpdateRequestDto(

    @Pattern(regexp = ".*\\S.*", message = "허브명은 공백일 수 없습니다.")
    String name,

    @Pattern(regexp = ".*\\S.*", message = "주소는 공백일 수 없습니다.")
    String address,

    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
    Double latitude,

    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
    Double longitude
) {
}
