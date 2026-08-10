package com.logistics.company.company.domain;

import com.logistics.company.global.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_companies")
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    @Builder
    public Company(UUID hubId, String name, String type, String address) {
        this.hubId = hubId;
        this.name = name;
        this.type = type;
        this.address = address;
    }

    public void update(UUID hubId, String name, String type, String address) {
        this.hubId = hubId;
        this.name = name;
        this.type = type;
        this.address = address;

    }
}
