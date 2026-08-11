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
import java.time.Instant;
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

    // 당일 전체 동선(허브→업체1→업체2→...) 총 거리(km)/시간(분) - 방문 순서 재계산 시마다 갱신
    private Integer totalDistance;

    private Integer totalDuration;

    private Instant routeComputedAt;

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

    // agentType/hubId는 그룹(정원·순번) 변경이 필요해 여기서 다루지 않음 - 호출 전에 변경 요청이 없는지 검증되어야 함
    public void update(
        String slackId,
        Boolean isAvailable
    ) {
        if (slackId != null) {
            this.slackId = slackId;
        }

        if (isAvailable != null) {
            this.isAvailable = isAvailable;
        }
    }

    public void updateRouteSummary(Integer totalDistance, Integer totalDuration, Instant computedAt) {
        this.totalDistance = totalDistance;
        this.totalDuration = totalDuration;
        this.routeComputedAt = computedAt;
    }

    @Override
    public void softDelete(UUID deletedBy) {
        super.softDelete(deletedBy);
    }

    public static void validateHubId(AgentType agentType, UUID hubId) {
        if (agentType == AgentType.COMPANY_DELIVERY && hubId == null) {
            throw new BusinessException(ErrorCode.HUB_ID_REQUIRED);
        }
    }
}
