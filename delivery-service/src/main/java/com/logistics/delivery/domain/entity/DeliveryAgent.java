package com.logistics.delivery.domain.entity;

import com.logistics.delivery.global.common.BaseUpdatableEntity;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
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
public class DeliveryAgent extends BaseUpdatableEntity {
    @Id
    @Column(name = "agent_id",updatable = false, nullable = false)
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
        validateHubId(agentType, hubId);
        this.id = agentId;
        this.hubId = hubId;
        this.agentType = agentType;
        this.slackId = slackId;
        this.deliveryOrder = deliveryOrder;
        this.isAvailable = isAvailable != null ? isAvailable : Boolean.TRUE;
    }

    public void update(
        UUID hubId,
        AgentType agentType,
        String slackId,
        Boolean isAvailable
    ) {

        AgentType targetType = agentType != null ? agentType : this.agentType;

        if (targetType == AgentType.HUB_DELIVERY) {
            this.hubId = null;
        } else if (hubId != null) {
            this.hubId = hubId;
        }

        validateHubId(targetType, this.hubId);
        this.agentType = targetType;

        // TODO: agentType/hubId가 바뀌면 deliveryOrder가 이전 그룹 기준 순번이라 새 그룹의 순번 규칙과 어긋날 수 있음 - 재계산 정책 필요
        if (slackId != null) {
            this.slackId = slackId;
        }

        if (isAvailable != null) {
            this.isAvailable = isAvailable;
        }
    }

    private void validateHubId(AgentType agentType, UUID hubId) {
        if (agentType == AgentType.COMPANY_DELIVERY && hubId == null) {
            throw new BusinessException(ErrorCode.HUB_ID_REQUIRED);
        }
    }
}
