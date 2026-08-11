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
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_delivery_route_records")
@Getter
public class DeliveryRouteRecord extends BaseUpdatableEntity {
    @Id
    @Column(name = "route_record_id",updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID deliveryId;

    @Column(nullable = false)
    private Integer sequence;

    @Column(nullable = false)
    private UUID departureHubId;

    @Column(nullable = false)
    private UUID arrivalHubId;

    @Column(nullable = false)
    private Integer estimatedDistance;

    @Column(nullable = false)
    private Integer estimatedDuration;

    private Integer actualDistance;

    private Integer actualDuration;

    @Enumerated(EnumType.STRING)
    @Column(length = 40, nullable = false)
    private RouteRecordStatus status;

    private UUID agentId;

    @Builder
    public DeliveryRouteRecord(
        UUID deliveryId,
        Integer sequence,
        UUID departureHubId,
        UUID arrivalHubId,
        Integer estimatedDistance,
        Integer estimatedDuration,
        Integer actualDistance,
        Integer actualDuration,
        UUID agentId
    ){
        this.deliveryId = deliveryId;
        this.sequence = sequence;
        this.departureHubId = departureHubId;
        this.arrivalHubId = arrivalHubId;
        this.estimatedDistance = estimatedDistance;
        this.estimatedDuration = estimatedDuration;
        this.actualDistance = actualDistance;
        this.actualDuration = actualDuration;
        this.status = RouteRecordStatus.WAITING;
        this.agentId = agentId;
    }

    public void assignAgent(UUID agentId) {
        this.agentId = agentId;
    }

    public void update(RouteRecordStatus status, Integer actualDistance, Integer actualDuration) {
        if (NEXT_STATUS.get(this.status) != status) {
            throw new BusinessException(ErrorCode.DELIVERY_ROUTE_RECORD_STATUS_NOT_CHANGEABLE);
        }
        if (status == RouteRecordStatus.ARRIVED && (actualDistance == null || actualDuration == null)) {
            throw new BusinessException(ErrorCode.DELIVERY_ROUTE_RECORD_ACTUAL_INFO_REQUIRED);
        }
        this.status = status;
        if (actualDistance != null) {
            this.actualDistance = actualDistance;
        }
        if (actualDuration != null) {
            this.actualDuration = actualDuration;
        }
    }

    @Override
    public void softDelete(UUID deletedBy) {
        super.softDelete(deletedBy);
    }

    // 상태 전이 규칙표: key(현재 상태) → value(허용되는 다음 상태). 역행/스킵 전이는 모두 이 맵에 없으므로 거부
    private static final Map<RouteRecordStatus, RouteRecordStatus> NEXT_STATUS = Map.of(
        RouteRecordStatus.WAITING, RouteRecordStatus.MOVING,
        RouteRecordStatus.MOVING, RouteRecordStatus.ARRIVED,
        RouteRecordStatus.ARRIVED, RouteRecordStatus.COMPLETED
    );
}
