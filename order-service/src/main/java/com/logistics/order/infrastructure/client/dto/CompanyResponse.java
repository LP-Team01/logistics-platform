package com.logistics.order.infrastructure.client.dto;

import java.util.UUID;

/**
 * 업체 조회 응답
 */
public record CompanyResponse(
        UUID companyId,
        UUID hubId,
        String address
) {
}
