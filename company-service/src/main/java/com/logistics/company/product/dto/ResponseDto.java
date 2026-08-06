package com.logistics.company.product.dto;

import com.logistics.company.product.domain.Product;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseDto (

    UUID productId,
    UUID companyId,
    UUID hubId,
    String name,
    Integer quantity,
    Integer price,
    LocalDateTime createdAt,
    String createdBy
){
    public static ResponseDto from(Product product) {
        return new ResponseDto(
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
