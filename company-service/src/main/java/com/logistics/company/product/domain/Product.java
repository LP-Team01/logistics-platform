package com.logistics.company.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_products")
public class Product {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "product_id", updatable = false, nullable = false)
    private UUID productId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 100, updatable = false)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    @Builder
    public Product(UUID companyId, UUID hubId, String name, Integer quantity, Integer price, String createdBy) {
        this.companyId = companyId;
        this.hubId = hubId;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, Integer quantity, Integer price, String updatedBy) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete(String deletedBy) {
        this.deletedBy = deletedBy;
        this.deletedAt = LocalDateTime.now();
    }
}
