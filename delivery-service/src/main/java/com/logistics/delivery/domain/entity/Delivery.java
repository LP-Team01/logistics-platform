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
@Table(name="p_deliveries")
@Getter
public class Delivery {
    // TODO: 감사필드 추가
    @Id
    @Column(name = "delivery_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(length = 40, nullable = false)
    private DeliveryStatus deliveryStatus;

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
        this.deliveryStatus = DeliveryStatus.HUB_WAITING;
        this.departureHubId = departureHubId;
        this.destinationHubId = destinationHubId;
        this.deliveryAddress = deliveryAddress;
        this.receiver = receiver;
        this.receiverSlackId = receiverSlackId;
        this.companyAgentId = companyAgentId;
    }
}
