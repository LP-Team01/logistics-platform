package com.logistics.delivery.infrastructure.kafka;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_delivery_ai_notification_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAiNotificationOutbox {

    @Id
    private UUID eventId;

    @Column(nullable = false, unique = true)
    private UUID deliveryId;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    public DeliveryAiNotificationOutbox(UUID eventId, UUID deliveryId, String payload) {
        this.eventId = eventId;
        this.deliveryId = deliveryId;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
    }
}