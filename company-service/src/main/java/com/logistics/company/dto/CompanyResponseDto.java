package com.logistics.company.dto;


import com.logistics.company.domain.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class CompanyResponseDto {

    private UUID companyId;
    private UUID hubId;
    private String name;
    private String type;
    private String address;
    private LocalDateTime createdAt;
    private String createdBy;

    // Entity -> DTO 변환 정적 팩토리 메서드
    public static CompanyResponseDto from(Company company) {
        return CompanyResponseDto.builder()
            .companyId(company.getCompanyId())
            .hubId(company.getHubId())
            .name(company.getName())
            .type(company.getType())
            .address(company.getAddress())
            .createdAt(company.getCreatedAt())
            .createdBy(company.getCreatedBy())
            .build();
    }
}
