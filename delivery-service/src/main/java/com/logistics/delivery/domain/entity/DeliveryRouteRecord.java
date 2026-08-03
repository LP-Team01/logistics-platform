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
@Table(name = "p_delivery_route_records")
@Getter
public class DeliveryRouteRecord {
    // TODO: 감사필드 추가
    @Id
    @Column(name = "route_record_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
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
}
