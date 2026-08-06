package com.logistics.company.product.controller;

import com.logistics.company.product.dto.CreateRequestDto;
import com.logistics.company.product.dto.ResponseDto;
import com.logistics.company.product.dto.UpdateRequestDto;
import com.logistics.company.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ResponseDto> createProduct(
        @Valid @RequestBody CreateRequestDto request,
        @RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymousUser") String userId
        ) {
        ResponseDto response = productService.createProduct(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ResponseDto> getProduct(@PathVariable("productId")UUID productId) {
        ResponseDto response = productService.getProduct(productId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ResponseDto> updateProduct(
        @PathVariable("productId") UUID productId,
        @Valid @RequestBody UpdateRequestDto request,
        @RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymousUser") String userId
        ) {
        ResponseDto response = productService.updateProduct(productId, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
        @PathVariable("productId") UUID productId,
        @RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymousUser") String userId
    ) {
        productService.deleteProduct(productId, userId);
        return ResponseEntity.noContent().build();
    }
}
