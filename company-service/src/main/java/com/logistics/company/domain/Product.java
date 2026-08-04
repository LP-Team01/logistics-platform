package com.logistics.company.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

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

    // Company Entity 객체를 직접 참조하지 않고 UUID ID만 보유
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    // BseEntity 감사(Audit) 필드
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
    public Product(UUID companyId, UUID hubId, String name, Integer stockQuantity,String createdBy) {
        this.companyId = companyId;
        this.hubId = hubId;
        this.name = name;
        this.stockQuantity = stockQuantity;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }


}
