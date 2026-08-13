package com.logistics.user.user.client;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyResponseDto(
    UUID companyId,
    UUID hubId,
    String name,
    String type,
    String address,
    LocalDateTime createdAt,
    String createdBy
) {
}
