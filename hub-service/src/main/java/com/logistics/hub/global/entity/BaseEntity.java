package com.logistics.hub.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 100, nullable = false)
    // todo: JWT 인증 완성되면 @CreatedBy로 전환 (user-service 의존)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by", length = 100)
    // todo: JWT 인증 완성되면 @LastModifiedBy로 전환
    private String updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    // todo: createdBy 처리 방식 확정되면 변경
    // - AuditorAware로 가면: 이 메서드 삭제, @CreatedBy 어노테이션으로 대체
    public void assignCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void assignUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void assignDeletedInfo(String deletedBy) {
        this.deletedAt = Instant.now();
        this.deletedBy = deletedBy;
    }
}
