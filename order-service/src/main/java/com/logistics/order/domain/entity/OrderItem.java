package com.logistics.order.domain.entity;

import com.logistics.order.global.entity.BaseEntity;
import com.logistics.order.global.exception.BusinessException;
import com.logistics.order.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "p_order_items")
public class OrderItem extends BaseEntity {

    // 주문 상품 한 줄의 고유 식별자입니다.
    @Id
    @Column(name = "order_item_id", nullable = false, updatable = false)
    private UUID orderItemId;

    /*
     * 여러 주문 상품이 하나의 주문에 속합니다.
     * LAZY를 사용하므로 OrderItem을 조회할 때 Order를 항상 함께 조회하지 않습니다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

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
    private Instant requestedDeadline;

    // 해당 상품 주문을 취소한 사용자의 UUID와 사유입니다.
    @Column(name = "canceled_by")
    private UUID canceledBy;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    // 상품을 공급하는 업체의 담당 허브
    @Column(name = "supplier_hub_id", nullable = false)
    private UUID supplierHubId;

    // JPA 전용 기본 생성자입니다.
    protected OrderItem() {
    }

    /**
     * 주문 상품을 생성합니다.
     * 잘못된 금액과 수량은 DB에 도달하기 전에 도메인에서 먼저 차단합니다.
     */
    private OrderItem(
        UUID productId,
        String productName,
        Long unitPrice,
        Integer quantity,
        UUID supplierCompanyId,
        UUID supplierHubId,
        Instant requestedDeadline,
        UUID createdBy
    ) {

        // 상품과 공급업체 정보 검증
        if (productId == null
                || supplierCompanyId == null
                || supplierHubId == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_REQUEST
            );
        }
        // 주문 단가 검증
        if (unitPrice == null || unitPrice < 0) throw new BusinessException(ErrorCode.INVALID_ORDER_UNIT_PRICE);
        // 주문 수량 검증
        if (quantity == null || quantity <= 0) throw new BusinessException(ErrorCode.INVALID_ORDER_QUANTITY);
        this.orderItemId = UUID.randomUUID();
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.supplierCompanyId = supplierCompanyId;
        this.supplierHubId = supplierHubId;
        this.status = OrderItemStatus.PENDING;
        // 곱셈 결과가 Long 범위를 넘으면 조용히 잘못된 값이 되지 않고 예외가 발생합니다.
        this.subtotal = Math.multiplyExact(unitPrice, quantity.longValue());
        this.requestedDeadline = requestedDeadline;
        initializeCreatedBy(createdBy);
    }

    /**
     * 주문 상품 취소
     */
    void cancel(UUID canceledBy, String cancelReason) {
        // 삭제된 상품 취소 방지
        if (isDeleted()) {
            throw new BusinessException(
                    ErrorCode.ORDER_ITEM_STATUS_NOT_CANCELED
            );
        }

        // 취소 정보 검증
        if (canceledBy == null
                || cancelReason == null
                || cancelReason.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_REQUEST
            );
        }

        // 중복 취소 방지
        if (this.status == OrderItemStatus.CANCELLED) {
            throw new BusinessException(
                    ErrorCode.ORDER_ITEM_ALREADY_CANCELED
            );
        }

        // 완료·실패 상품은 취소 불가
        if (this.status == OrderItemStatus.COMPLETED
                || this.status == OrderItemStatus.FAILED) {
            throw new BusinessException(
                    ErrorCode.ORDER_ITEM_STATUS_NOT_CANCELED
            );
        }

        this.status = OrderItemStatus.CANCELLED;
        this.canceledBy = canceledBy;
        this.cancelReason = cancelReason;
    }

    /**
     * 생성된 배송 연결
     */
    public void assignDelivery(UUID deliveryId) {
        if (deliveryId == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_DELIVERY_ID
            );
        }

        // 같은 배송의 재처리는 허용
        if (deliveryId.equals(this.deliveryId)) {
            return;
        }

        // 다른 배송으로 덮어쓰기 방지
        if (this.deliveryId != null) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_ALREADY_ASSIGNED
            );
        }

        // 취소·완료·실패 상품에는 배송 연결 불가
        if (this.status == OrderItemStatus.CANCELLED
                || this.status == OrderItemStatus.COMPLETED
                || this.status == OrderItemStatus.FAILED) {
            throw new BusinessException(
                    ErrorCode.ORDER_ITEM_STATUS_NOT_CHANGEABLE
            );
        }

        this.deliveryId = deliveryId;
        this.status = OrderItemStatus.DELIVERY_CREATED;
    }

    /**
     * 주문 상품 배송 완료
     */
    void complete() {
        // 삭제된 상품은 완료 처리 불가
        if (isDeleted()) {
            throw new BusinessException(
                    ErrorCode.ORDER_ITEM_STATUS_NOT_CHANGEABLE
            );
        }

        // 배송이 생성된 상품만 완료 가능
        if (this.status != OrderItemStatus.DELIVERY_CREATED) {
            throw new BusinessException(
                    ErrorCode.ORDER_ITEM_STATUS_NOT_CHANGEABLE
            );
        }

        this.status = OrderItemStatus.COMPLETED;
    }

    public static OrderItem create(
        UUID productId,
        String productName,
        Long unitPrice,
        Integer quantity,
        UUID supplierCompanyId,
        UUID supplierHubId,
        Instant requestedDeadline,
        UUID createdBy
    ) {
        return new OrderItem(
            productId,
            productName,
            unitPrice,
            quantity,
            supplierCompanyId,
            supplierHubId,
            requestedDeadline,
            createdBy
        );
    }

    /** Order.addItem()에서만 호출해 양방향 연관관계가 한쪽만 설정되는 것을 방지합니다. */
    void assignOrder(Order order) { this.order = order; }

    public UUID getOrderItemId() { return orderItemId; }
    public UUID getProductId() { return productId; }
    public UUID getSupplierHubId() { return supplierHubId;}
    public String getProductName() { return productName; }
    public Long getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
    public UUID getSupplierCompanyId() { return supplierCompanyId; }
    public UUID getDeliveryId() { return deliveryId; }
    public OrderItemStatus getStatus() { return status; }
    public Long getSubtotal() { return subtotal; }
    public Instant getRequestedDeadline() { return requestedDeadline; }
}
