package com.logistics.order.infrastructure.client;

import com.logistics.order.infrastructure.client.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Company Service 상품 조회
 */
@FeignClient(
        name = "company-service",
        contextId = "productClient",
        path = "/api/products"
)
public interface ProductClient {

    /**
     * 상품 정보 조회
     */
    @GetMapping("/{productId}")
    ProductResponse getProduct(
            @PathVariable UUID productId
    );
}
