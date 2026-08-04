package com.logistics.order.domain.entity;

import com.logistics.order.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "p_order")
public class Order extends BaseEntity {

    // 주문의 고유 식별자입니다. Hibernate가 UUID를 자동 생성합니다.
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    // 상품을 실제로 받을 업체의 ID입니다. Company Service의 업체를 논리적으로 참조합니다.
    @Column(name = "receiver_company_id", nullable = false)
    private UUID receiverCompanyId;

    // 주문 전체의 진행 상태입니다. DB에는 Enum 이름이 문자열로 저장됩니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    // 주문에 포함된 모든 상품의 subtotal 합계입니다.
    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    // 문 앞 배송처럼 주문자가 남긴 배송 요청사항입니다.
    @Column(name = "delivery_request", columnDefinition = "TEXT")
    private String deliveryRequest;

    // 주문을 취소한 사용자의 ID와 취소 사유입니다. 명세에 따라 취소자 ID는 BIGINT를 사용합니다.
    @Column(name = "canceled_by")
    private Long canceledBy;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    /*
     * 주문 하나는 여러 주문 상품을 가집니다.
     * Order를 저장하면 새 OrderItem도 함께 저장되고,
     * 목록에서 제거한 OrderItem은 DB에서도 삭제됩니다.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OrderItem> orderItems = new ArrayList<>();

    // JPA가 DB 조회 결과를 객체로 만들 때 사용하는 생성자입니다. 외부에서 직접 호출하지 않습니다.
    protected Order() {
    }

    // 주문 생성은 생성자를 직접 노출하지 않고 create() 메서드로만 수행합니다.
    private Order(UUID receiverCompanyId, String deliveryRequest, UUID createdBy) {
        this.receiverCompanyId = receiverCompanyId;
        this.status = OrderStatus.PENDING;
        this.totalAmount = 0L;
        this.deliveryRequest = deliveryRequest;
        initializeCreatedBy(createdBy);
    }

    /**
     * 새로운 주문을 만듭니다.
     * 최초 상태는 PENDING이고 상품이 아직 없으므로 총액은 0원입니다.
     */
    public static Order create(UUID receiverCompanyId, String deliveryRequest, UUID createdBy) {
        return new Order(receiverCompanyId, deliveryRequest, createdBy);
    }

    /**
     * 주문에 상품 하나를 추가합니다.
     * OrderItem에 현재 주문을 연결하고 주문 총액도 다시 계산합니다.
     */
    public void addItem(OrderItem orderItem) {
        orderItem.assignOrder(this);
        orderItems.add(orderItem);
        recalculateTotalAmount();
    }

    /** 주문을 취소 상태로 변경하고 취소 정보를 기록합니다. */
    public void cancel(Long canceledBy, String cancelReason) {
        this.status = OrderStatus.CANCELLED;
        this.canceledBy = canceledBy;
        this.cancelReason = cancelReason;
    }

    // 상품이 추가될 때마다 DB 값에 의존하지 않고 현재 상품 목록으로 총액을 계산합니다.
    private void recalculateTotalAmount() {
        this.totalAmount = orderItems.stream().mapToLong(OrderItem::getSubtotal).sum();
    }

    public UUID getOrderId() { return orderId; }
    public UUID getReceiverCompanyId() { return receiverCompanyId; }
    public OrderStatus getStatus() { return status; }
    public Long getTotalAmount() { return totalAmount; }
    public String getDeliveryRequest() { return deliveryRequest; }
    public Long getCanceledBy() { return canceledBy; }
    public String getCancelReason() { return cancelReason; }
    public List<OrderItem> getOrderItems() { return Collections.unmodifiableList(orderItems); }
}
