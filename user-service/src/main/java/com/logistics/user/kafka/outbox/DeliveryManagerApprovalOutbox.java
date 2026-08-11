package com.logistics.user.kafka.outbox;

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
@Table(name = "p_delivery_manager_approval_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryManagerApprovalOutbox {

    @Id
    private UUID eventId;

    @Column(nullable = false)
    private UUID agentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @Column(nullable = false)
    private int attemptCount;

    public DeliveryManagerApprovalOutbox(UUID eventId, UUID agentId, String payload) {
        this.eventId = eventId;
        this.agentId = agentId;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.attemptCount = 0;
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
    }

    public void incrementAttempt() {
        this.attemptCount++;
    }
}
