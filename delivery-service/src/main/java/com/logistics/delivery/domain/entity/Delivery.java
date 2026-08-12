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
import jakarta.persistence.Version;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_deliveries")
@Getter
public class Delivery extends BaseUpdatableEntity {

    @Id
    @Column(name = "delivery_id",updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    // 주문아이템 ID. 아이템마다 다른 허브를 거칠 수 있어 주문 단위가 아닌 아이템 단위로 배송 생성.
    @Column(nullable = false)
    private UUID orderItemId;

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

    // DESTINATION_ARRIVED 전이가 업체배송담당자 배정(회전 커서 소비)을 동반하므로, 같은 배송 건에 대한 동시 상태 변경
    // 요청 중 하나만 성공시키기 위한 낙관적 락. 두 번째 요청은 DELIVERY_STATUS_UPDATE_CONFLICT(409)로 거부된다.
    @Version
    @Column(nullable = false)
    private Long version;

    @Builder
    public Delivery(
        UUID orderId,
        UUID orderItemId,
        UUID departureHubId,
        UUID destinationHubId,
        String deliveryAddress,
        String receiver,
        String receiverSlackId,
        UUID companyAgentId
    ) {
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.status = DeliveryStatus.HUB_WAITING;
        this.departureHubId = departureHubId;
        this.destinationHubId = destinationHubId;
        this.deliveryAddress = deliveryAddress;
        this.receiver = receiver;
        this.receiverSlackId = receiverSlackId;
        this.companyAgentId = companyAgentId;
    }

    public void assignCompanyAgent(UUID companyAgentId) {
        this.companyAgentId = companyAgentId;
    }

    public void update(DeliveryStatus status) {
        if (NEXT_STATUS.get(this.status) != status) {
            throw new BusinessException(ErrorCode.DELIVERY_STATUS_NOT_CHANGEABLE);
        }
        this.status = status;
    }

    @Override
    public void softDelete(UUID deletedBy) {
        super.softDelete(deletedBy);
    }

    // 상태 전이 규칙표: key(현재 상태) → value(허용되는 다음 상태). 역행/스킵 전이는 모두 이 맵에 없으므로 거부
    private static final Map<DeliveryStatus, DeliveryStatus> NEXT_STATUS = Map.of(
        DeliveryStatus.HUB_WAITING, DeliveryStatus.HUB_MOVING,
        DeliveryStatus.HUB_MOVING, DeliveryStatus.DESTINATION_ARRIVED,
        DeliveryStatus.DESTINATION_ARRIVED, DeliveryStatus.DELIVERING,
        DeliveryStatus.DELIVERING, DeliveryStatus.COMPANY_MOVING,
        DeliveryStatus.COMPANY_MOVING, DeliveryStatus.DELIVERED
    );
}
