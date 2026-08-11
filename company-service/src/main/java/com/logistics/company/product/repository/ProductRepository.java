package com.logistics.company.product.repository;

import com.logistics.company.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByProductIdAndDeletedAtIsNull(UUID productId);

    List<Product> findAllByProductIdInAndDeletedAtIsNull(List<UUID> productIds);
}
