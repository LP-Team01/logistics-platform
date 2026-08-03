package com.logistics.delivery.domain.entity;

import com.logistics.delivery.global.common.BaseUpdatableEntity;
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
@Table(name="p_deliveries")
@Getter
public class Delivery extends BaseUpdatableEntity {

    @Id
    @Column(name = "delivery_id",updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(length = 40, nullable = false)
    private DeliveryStatus status;

    @Column(nullable = false)
    private UUID departureHubId;

    @Column(nullable = false)
    private UUID destinationHubId;

    @Column(nullable = false)
    private String deliveryAddress;

    @Column(length = 100, nullable = false)
    private String receiver;

    @Column(length = 50)
    private String receiverSlackId;

    private UUID companyAgentId;

    @Builder
    public Delivery(
        UUID orderId,
        UUID departureHubId,
        UUID destinationHubId,
        String deliveryAddress,
        String receiver,
        String receiverSlackId,
        UUID companyAgentId
    ){
        this.orderId = orderId;
        this.status = DeliveryStatus.HUB_WAITING;
        this.departureHubId = departureHubId;
        this.destinationHubId = destinationHubId;
        this.deliveryAddress = deliveryAddress;
        this.receiver = receiver;
        this.receiverSlackId = receiverSlackId;
        this.companyAgentId = companyAgentId;
    }
}
