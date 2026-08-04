package com.logistics.company.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class CompanyUpdateRequest {

    @NotNull(message = "관리 허브 ID는 필수입니다.")
    private UUID hubId;

    @NotBlank(message = "업체명은 필수입니다.")
    private String name;

    @NotBlank(message = "업체 타입은 필수입니다.")
    private String type;

    @NotBlank(message = "업체 주소는 필수입니다.")
    private String address;

}
