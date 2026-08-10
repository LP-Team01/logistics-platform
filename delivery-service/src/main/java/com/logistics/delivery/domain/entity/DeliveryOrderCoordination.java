package com.logistics.delivery.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 같은 주문의 배송 생성과 취소 순서를 DB 잠금으로 조정합니다. */
@Entity
@Table(name = "p_delivery_order_coordination")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryOrderCoordination {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "cancelled", nullable = false)
    private boolean cancelled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public void cancel() {
        this.cancelled = true;
    }
}
