package com.logistics.delivery.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_delivery_agents")
@Getter
public class DeliveryAgent {
    // TODO: 감사필드 추가
    @Id
    @Column(name = "agent_id")
    private UUID id;

    private UUID hubId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private AgentType agentType;

    @Column(length = 100)
    private String slackId;

    @Column(nullable = false)
    private Integer deliveryOrder;

    @Column(nullable = false)
    private boolean isAvailable = Boolean.TRUE;

    @Builder
    public DeliveryAgent(
        UUID agentId,
        UUID hubId,
        AgentType agentType,
        String slackId,
        Integer deliveryOrder,
        Boolean isAvailable
    ){
        this.id = agentId;
        this.hubId = hubId;
        this.agentType = agentType;
        this.slackId = slackId;
        this.deliveryOrder = deliveryOrder;
        this.isAvailable = isAvailable != null ? isAvailable : Boolean.TRUE;
    }
}
