package com.logistics.company.product.dto;

import com.logistics.company.product.domain.Product;

import java.util.UUID;

public record ProductBatchResponseDto(
    UUID productId,
    String productName,
    Integer unitPrice,
    UUID supplierCompanyId
) {
    public static ProductBatchResponseDto from(Product product) {
        return new ProductBatchResponseDto(
            product.getProductId(),
            product.getName(),
            product.getPrice(),
            product.getCompanyId()
        );
    }
}
