package com.logistics.company.product.service;

import com.logistics.company.company.domain.Company;
import com.logistics.company.company.repository.CompanyRepository;
import com.logistics.company.global.exception.BusinessException;
import com.logistics.company.global.exception.ErrorCode;
import com.logistics.company.product.domain.Product;
import com.logistics.company.product.dto.ProductCreateRequestDto;
import com.logistics.company.product.dto.ProductUpdateRequestDto;
import com.logistics.company.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CompanyRepository companyRepository;

    private UUID companyId;
    private UUID hubId;
    private UUID productId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        hubId = UUID.randomUUID();
        productId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("존재하지 않는 companyId로 상품 생성 시 COMPANY_NOT_FOUND 예외 발생")
    void createProduct_notFoundCompany_throwsException() {
        // given
        ProductCreateRequestDto request = new ProductCreateRequestDto(companyId, hubId, "상품A", 10, 10000);
        given(companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.createProduct(request, userId))
            .isInstanceOf(BusinessException.class)
            .hasMessage(ErrorCode.COMPANY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("업체의 hubId와 요청된 hubId가 다르면 INVALID_HUB_MISMATCH 예외 발생")
    void createProduct_mismatchedHubId_throwsException() {
        // given
        UUID differentHubId = UUID.randomUUID(); // 업체 허브와 다른 허브 ID
        ProductCreateRequestDto request = new ProductCreateRequestDto(companyId, differentHubId, "상품A", 10, 10000);

        Company company = Company.builder()
            .hubId(hubId) // 실제 업체에 연결된 허브 ID
            .name("테스트업체")
            .type("SUPPLIER")
            .address("서울")
            .build();

        given(companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId))
            .willReturn(Optional.of(company));

        // when & then
        assertThatThrownBy(() -> productService.createProduct(request, userId))
            .isInstanceOf(BusinessException.class)
            .hasMessage(ErrorCode.INVALID_HUB_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("논리 삭제된 상품 조회 시 PRODUCT_NOT_FOUND 예외 발생")
    void getProduct_deletedProduct_throwsException() {
        // given
        given(productRepository.findByProductIdAndDeletedAtIsNull(productId))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(productId))
            .isInstanceOf(BusinessException.class)
            .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("삭제된 상품 수정 시 PRODUCT_NOT_FOUND 예외 발생")
    void updateProduct_deletedProduct_throwsException() {
        // given
        ProductUpdateRequestDto request = new ProductUpdateRequestDto("수정상품", 20, 20000);
        given(productRepository.findByProductIdAndDeletedAtIsNull(productId))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.updateProduct(productId, request, userId))
            .isInstanceOf(BusinessException.class)
            .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("삭제된 상품 재삭제 시 PRODUCT_NOT_FOUND 예외 발생")
    void deleteProduct_alreadyDeleted_throwsException() {
        // given
        given(productRepository.findByProductIdAndDeletedAtIsNull(productId))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.deleteProduct(productId, userId))
            .isInstanceOf(BusinessException.class)
            .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }
}
