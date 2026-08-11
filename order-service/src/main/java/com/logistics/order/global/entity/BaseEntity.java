package com.logistics.order.global.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;

/**
 * 모든 도메인 Entity가 상속하는 공통 audit 필드.
 * - 생성/수정/삭제 시각 및 수행자를 기록한다.
 * - 실제 delete()가 아닌 Soft Delete(deletedAt/deletedBy) 방식을 사용한다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    protected void initializeCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Spring Data JPA의 @LastModifiedDate/@LastModifiedBy는 스펙상 생성 시점에도 자동으로 채워진다.
     * 하지만 "진짜 수정이 일어나기 전까지는 null이어야 한다"는 요구사항이 있어,
     * AuditingEntityListener(@PrePersist)가 먼저 채운 값을 저장 직전에 다시 비운다.
     * (JPA 콜백 순서상 @EntityListeners가 엔티티 자신의 @PrePersist보다 먼저 실행됨)
     */
    @PrePersist
    private void clearUpdatedAuditOnCreate() {
        this.updatedAt = null;
        this.updatedBy = null;
    }

    /**
     * Soft Delete 처리. 실제 row는 삭제하지 않는다.
     * 예) orderRepository.findById(id).ifPresent(o -> o.softDelete(currentUserId));
     */
    public void softDelete(UUID deletedByUserId) {
        this.deletedAt = Instant.now();
        this.deletedBy = deletedByUserId;
    }

    /** 삭제 취소(복구)가 필요한 경우 사용. */
    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
