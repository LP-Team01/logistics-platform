package com.logistics.order.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_delivery_compensation_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryCompensationOutbox {

    @Id
    private UUID eventId;

    @Column(nullable = false, unique = true)
    private UUID orderId;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    public DeliveryCompensationOutbox(UUID orderId) {
        this.eventId = UUID.randomUUID();
        this.orderId = orderId;
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
    }
}
