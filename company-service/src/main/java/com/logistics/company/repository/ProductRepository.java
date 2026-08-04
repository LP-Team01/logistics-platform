package com.logistics.company.repository;

import com.logistics.company.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    // 삭제되지 않은 상품만 조회
    Optional<Product> findByProductIdAndDeletedAtIsNull(UUID productId);
}
