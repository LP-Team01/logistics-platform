package com.logistics.order.infrastructure.client.dto;

import java.util.UUID;

/**
 * 상품 조회 응답
 */
public record ProductResponse(
        UUID productId,
        String productName,
        Long unitPrice,
        UUID supplierCompanyId
) {
}
