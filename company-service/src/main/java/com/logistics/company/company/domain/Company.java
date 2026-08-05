package com.logistics.company.company.domain;

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
@Table(name = "p_companies")
public class Company {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "company_id", updatable = false, nullable = false)
    private UUID companyId;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

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
    public Company(UUID hubId, String name, String type, String address, String createdBy) {
        this.hubId = hubId;
        this.name = name;
        this.type = type;
        this.address = address;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public void update(UUID hubId, String name, String type, String address, String updatedBy) {
        this.hubId = hubId;
        this.name = name;
        this.type = type;
        this.address = address;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete(String deletedBy) {
        this.deletedBy = deletedBy;
        this.deletedAt = LocalDateTime.now();
    }
}
