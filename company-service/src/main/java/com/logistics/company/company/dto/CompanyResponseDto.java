package com.logistics.company.company.dto;

import com.logistics.company.company.domain.Company;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyResponseDto(
    UUID companyId,
    UUID hubId,
    String name,
    String type,
    String address,
    Instant createdAt,
    UUID createdBy
) {
    // Entity -> DTO 변환 정적 팩토리 메서드
    public static CompanyResponseDto from(Company company) {
        return new CompanyResponseDto(
            company.getCompanyId(),
            company.getHubId(),
            company.getName(),
            company.getType(),
            company.getAddress(),
            company.getCreatedAt(),
            company.getCreatedBy()
        );
    }
}
