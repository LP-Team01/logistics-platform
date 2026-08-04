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
@Table(name = "p_company_delivery_route_records")
@Getter
public class CompanyDeliveryRouteRecord extends BaseUpdatableEntity {
    @Id
    @Column(name = "record_id",updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID deliveryId;

    @Column(nullable = false)
    private UUID departureHubId;

    @Column(nullable = false)
    private UUID receiverCompanyId;

    private Integer estimatedDistance;

    private Integer estimatedDuration;

    private Integer actualDistance;

    private Integer actualDuration;

    @Enumerated(EnumType.STRING)
    @Column(length = 40, nullable = false)
    private CompanyRouteRecordStatus status;

    private UUID agentId;

    private Integer deliverySequence;

    @Builder
    public CompanyDeliveryRouteRecord(
        UUID deliveryId,
        UUID departureHubId,
        UUID receiverCompanyId,
        Integer estimatedDistance,
        Integer estimatedDuration,
        Integer actualDistance,
        Integer actualDuration,
        UUID agentId,
        Integer deliverySequence
    ){
        this.deliveryId = deliveryId;
        this.departureHubId = departureHubId;
        this.receiverCompanyId = receiverCompanyId;
        this.estimatedDistance = estimatedDistance;
        this.estimatedDuration = estimatedDuration;
        this.actualDistance = actualDistance;
        this.actualDuration = actualDuration;
        this.status = CompanyRouteRecordStatus.WAITING;
        this.agentId = agentId;
        this.deliverySequence = deliverySequence;
    }

    public void update(CompanyRouteRecordStatus status, Integer actualDistance, Integer actualDuration) {
        if (NEXT_STATUS.get(this.status) != status) {
            throw new BusinessException(ErrorCode.COMPANY_ROUTE_RECORD_STATUS_NOT_CHANGEABLE);
        }
        if (status == CompanyRouteRecordStatus.DELIVERED && (actualDistance == null || actualDuration == null)) {
            throw new BusinessException(ErrorCode.COMPANY_ROUTE_RECORD_ACTUAL_INFO_REQUIRED);
        }
        this.status = status;
        if (actualDistance != null) {
            this.actualDistance = actualDistance;
        }
        if (actualDuration != null) {
            this.actualDuration = actualDuration;
        }
    }

    // 상태 전이 규칙표: key(현재 상태) → value(허용되는 다음 상태). 역행/스킵 전이는 모두 이 맵에 없으므로 거부
    private static final Map<CompanyRouteRecordStatus, CompanyRouteRecordStatus> NEXT_STATUS = Map.of(
        CompanyRouteRecordStatus.WAITING, CompanyRouteRecordStatus.COMPANY_MOVING,
        CompanyRouteRecordStatus.COMPANY_MOVING, CompanyRouteRecordStatus.DELIVERED
    );
}
