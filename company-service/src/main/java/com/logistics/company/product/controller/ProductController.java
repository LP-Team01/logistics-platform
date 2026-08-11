package com.logistics.company.product.controller;

import com.logistics.company.product.dto.ProductBatchResponseDto;
import com.logistics.company.product.dto.ProductCreateRequestDto;
import com.logistics.company.product.dto.ProductResponseDto;
import com.logistics.company.product.dto.ProductUpdateRequestDto;
import com.logistics.company.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(
        @Valid @RequestBody ProductCreateRequestDto request,
        @RequestHeader(value = "X-User-Id") UUID userId
    ) {
        ProductResponseDto response = productService.createProduct(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ProductBatchResponseDto>> getProductsBatch(
        @RequestBody List<UUID> productIds
    ) {
        List<ProductBatchResponseDto> response = productService.getProductsBatch(productIds);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProduct(@PathVariable("productId") UUID productId) {
        ProductResponseDto response = productService.getProduct(productId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> updateProduct(
        @PathVariable("productId") UUID productId,
        @Valid @RequestBody ProductUpdateRequestDto request,
        @RequestHeader(value = "X-User-Id") UUID userId
    ) {
        ProductResponseDto response = productService.updateProduct(productId, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
        @PathVariable("productId") UUID productId,
        @RequestHeader(value = "X-User-Id") UUID userId
    ) {
        productService.deleteProduct(productId, userId);
        return ResponseEntity.noContent().build();
    }
}
