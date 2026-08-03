package com.logistics.hub.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_hub_routes")
public class HubRoute extends BaseEntity {

    @Id
    @Column(name = "hub_route_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID hubRouteId;

    @Column(name = "departure_hub_id", nullable = false)
    private UUID departureHubId;

    @Column(name = "arrival_hub_id", nullable = false)
    private UUID arrivalHubId;

    @Column(nullable = false)
    private Double distance;

    @Column(nullable = false)
    private Integer duration;

    @Builder
    public HubRoute(UUID departureHubId, UUID arrivalHubId, Double distance, Integer duration) {
        this.departureHubId = departureHubId;
        this.arrivalHubId = arrivalHubId;
        this.distance = distance;
        this.duration = duration;
    }
}
