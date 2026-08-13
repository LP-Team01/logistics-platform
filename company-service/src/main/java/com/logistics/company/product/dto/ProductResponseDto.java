package com.logistics.company.product.dto;

import com.logistics.company.product.domain.Product;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponseDto(

    UUID productId,
    UUID companyId,
    UUID hubId,
    String name,
    Integer quantity,
    Integer price,
    Instant createdAt,
    UUID createdBy
){
    public static ProductResponseDto from(Product product) {
        return new ProductResponseDto(
            product.getProductId(),
            product.getCompanyId(),
            product.getHubId(),
            product.getName(),
            product.getQuantity(),
            product.getPrice(),
            product.getCreatedAt(),
            product.getCreatedBy()
        );
    }
}
