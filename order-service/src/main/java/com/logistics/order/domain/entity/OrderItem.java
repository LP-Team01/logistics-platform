package com.logistics.order.domain.entity;

import com.logistics.order.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_order_items")
public class OrderItem extends BaseEntity {

    // 주문 상품 한 줄의 고유 식별자입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "order_item_id", nullable = false, updatable = false)
    private UUID orderItemId;

    /*
     * 여러 주문 상품이 하나의 주문에 속합니다.
     * LAZY를 사용하므로 OrderItem을 조회할 때 Order를 항상 함께 조회하지 않습니다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order orderId;

    // 주문 당시 선택한 상품 ID입니다. Product Service의 상품을 논리적으로 참조합니다.
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    // 상품명이 나중에 바뀌더라도 주문 당시 이름을 보존하기 위한 스냅샷입니다.
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    // 상품의 현재 가격이 아니라 주문 당시 단가입니다.
    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    // 주문 수량은 1개 이상이어야 합니다.
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // 상품을 공급하는 업체 ID입니다.
    @Column(name = "supplier_company_id", nullable = false)
    private UUID supplierCompanyId;

    // Delivery Service에서 배송이 생성된 후 받은 ID입니다. 상품별 배송은 하나만 연결됩니다.
    @Column(name = "delivery_id", unique = true)
    private UUID deliveryId;

    // 주문 상품 단위의 처리 상태입니다. 전체 주문 상태와 별도로 관리합니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderItemStatus status;

    // 주문 당시 단가 × 수량으로 계산한 상품별 금액입니다.
    @Column(name = "subtotal", nullable = false)
    private Long subtotal;

    // 주문자가 요청한 납품 완료 희망 시각입니다.
    @Column(name = "requested_deadline", nullable = false)
    private LocalDateTime requestedDeadline;

    // 해당 상품 주문을 취소한 사용자의 UUID와 사유입니다.
    @Column(name = "canceled_by")
    private UUID canceledBy;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    // JPA 전용 기본 생성자입니다.
    protected OrderItem() {
    }

    /**
     * 주문 상품을 생성합니다.
     * 잘못된 금액과 수량은 DB에 도달하기 전에 도메인에서 먼저 차단합니다.
     */
    private OrderItem(UUID productId, String productName, Long unitPrice, Integer quantity,
            UUID supplierCompanyId, LocalDateTime requestedDeadline, UUID createdBy) {
        if (unitPrice == null || unitPrice < 0) throw new IllegalArgumentException("단가는 0 이상이어야 합니다.");
        if (quantity == null || quantity <= 0) throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다.");
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.supplierCompanyId = supplierCompanyId;
        this.status = OrderItemStatus.PENDING;
        // 곱셈 결과가 Long 범위를 넘으면 조용히 잘못된 값이 되지 않고 예외가 발생합니다.
        this.subtotal = Math.multiplyExact(unitPrice, quantity.longValue());
        this.requestedDeadline = requestedDeadline;
        initializeCreatedBy(createdBy);
    }

    public static OrderItem create(UUID productId, String productName, Long unitPrice, Integer quantity,
            UUID supplierCompanyId, LocalDateTime requestedDeadline, UUID createdBy) {
        return new OrderItem(productId, productName, unitPrice, quantity,
                supplierCompanyId, requestedDeadline, createdBy);
    }

    /** Order.addItem()에서만 호출해 양방향 연관관계가 한쪽만 설정되는 것을 방지합니다. */
    void assignOrder(Order orderId) { this.orderId = orderId; }

    public UUID getOrderItemId() { return orderItemId; }
    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Long getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
    public UUID getSupplierCompanyId() { return supplierCompanyId; }
    public UUID getDeliveryId() { return deliveryId; }
    public OrderItemStatus getStatus() { return status; }
    public Long getSubtotal() { return subtotal; }
    public LocalDateTime getRequestedDeadline() { return requestedDeadline; }
}
