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
@Table(name = "p_delivery_agent_approval_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAgentApprovalOutbox {

    @Id
    private UUID eventId;

    // 재승인 등으로 같은 agentId에 대한 결과 이벤트가 여러 번 발행될 수 있어 UNIQUE 제약은 두지 않는다.
    @Column(nullable = false)
    private UUID agentId;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    public DeliveryAgentApprovalOutbox(UUID eventId, UUID agentId, String payload) {
        this.eventId = eventId;
        this.agentId = agentId;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
    }
}