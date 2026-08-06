package com.logistics.company.product.service;

import com.logistics.company.global.exception.BusinessException;
import com.logistics.company.global.exception.ErrorCode;
import com.logistics.company.product.domain.Product;
import com.logistics.company.product.dto.CreateRequestDto;
import com.logistics.company.product.dto.ResponseDto;
import com.logistics.company.product.dto.UpdateRequestDto;
import com.logistics.company.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService  {

    private final ProductRepository productRepository;
    // TODO : 추후 OpenFeign을 통한 허브 및 업체 존재 유효성 검증 추가 위치

    @Transactional
    public ResponseDto createProduct(CreateRequestDto request, String userId) {
        validateHubAndCompanyExists(request.hubId(), request.companyId());

        Product product = Product.builder()
            .companyId(request.companyId())
            .hubId(request.hubId())
            .name(request.name())
            .quantity(request.quantity())
            .price(request.price())
            .createdBy(userId)
            .build();

        Product savedProduct = productRepository.save(product);
        return ResponseDto.from(product);
    }

    public ResponseDto getProduct(UUID productId) {
        Product product = productRepository.findByProductIdAndDeletedAtIsNull(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        return ResponseDto.from(product);
    }

    @Transactional
    public ResponseDto updateProduct (UUID productId, UpdateRequestDto request, String userId) {
        Product product = productRepository.findByProductIdAndDeletedAtIsNull(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.update(request.name(), request.quantity(), request.price(), userId);
        return ResponseDto.from(product);
    }

    @Transactional
    public void deleteProduct(UUID productId, String userId) {
        Product product = productRepository.findByProductIdAndDeletedAtIsNull(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.delete(userId);
    }

    private void validateHubAndCompanyExists(UUID hubId, UUID companyId) {
        // TODO : OpenFeign 통신으로 허브 및 업체 존재 여부 검증 로직 작성 예정
    }
}
