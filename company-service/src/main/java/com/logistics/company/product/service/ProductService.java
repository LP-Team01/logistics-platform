package com.logistics.company.product.service;

import com.logistics.company.company.domain.Company;
import com.logistics.company.company.repository.CompanyRepository;
import com.logistics.company.global.exception.BusinessException;
import com.logistics.company.global.exception.ErrorCode;
import com.logistics.company.product.domain.Product;
import com.logistics.company.product.dto.ProductBatchResponseDto;
import com.logistics.company.product.dto.ProductCreateRequestDto;
import com.logistics.company.product.dto.ProductResponseDto;
import com.logistics.company.product.dto.ProductUpdateRequestDto;
import com.logistics.company.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public ProductResponseDto createProduct(ProductCreateRequestDto request, UUID userId) {
        validateHubAndCompanyExists(request.hubId(), request.companyId());

        Product product = Product.builder()
            .companyId(request.companyId())
            .hubId(request.hubId())
            .name(request.name())
            .quantity(request.quantity())
            .price(request.price())
            .build();

        Product savedProduct = productRepository.save(product);
        return ProductResponseDto.from(savedProduct);
    }

    public ProductResponseDto getProduct(UUID productId) {
        Product product = productRepository.findByProductIdAndDeletedAtIsNull(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductResponseDto.from(product);
    }


    public List<ProductBatchResponseDto> getProductsBatch(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty() || productIds.size() > 20) {
            throw new BusinessException(ErrorCode.INVALID_BATCH_SIZE);
        }

        long uniqueCount = productIds.stream().distinct().count();
        if (uniqueCount != productIds.size()) {
            throw new BusinessException(ErrorCode.DUPLICATE_PRODUCT_ID);
        }

        List<Product> products = productRepository.findAllByProductIdInAndDeletedAtIsNull(productIds);

        if (products.size() != productIds.size()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return products.stream()
            .map(ProductBatchResponseDto::from)
            .toList();
    }

    @Transactional
    public ProductResponseDto updateProduct(UUID productId, ProductUpdateRequestDto request, UUID userId) {
        Product product = productRepository.findByProductIdAndDeletedAtIsNull(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        product.update(request.name(), request.quantity(), request.price());
        return ProductResponseDto.from(product);
    }

    @Transactional
    public void deleteProduct(UUID productId, UUID userId) {
        Product product = productRepository.findByProductIdAndDeletedAtIsNull(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        product.delete(userId.toString());
    }

    private void validateHubAndCompanyExists(UUID hubId, UUID companyId) {
        Company company = companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        if (!company.getHubId().equals(hubId)) {
            throw new BusinessException(ErrorCode.INVALID_HUB_MISMATCH);
        }

        // TODO : 추후 OpenFeign 통신으로 hub-service에 hubId 실제 존재 여부 확인 추가 예정
    }
}
