package com.logistics.order.domain.entity;

import com.logistics.order.global.entity.BaseEntity;
import com.logistics.order.global.exception.BusinessException;
import com.logistics.order.global.exception.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "p_orders")
public class Order extends BaseEntity implements Persistable<UUID> {

    // 주문의 고유 식별자입니다. Hibernate가 UUID를 자동 생성합니다.
    @Id
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

    // 주문을 취소한 사용자의 UUID와 취소 사유입니다.
    @Column(name = "canceled_by")
    private UUID canceledBy;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    /**
     * 수령업체 담당 허브 ID
     */
    @Column(name = "receiver_hub_id", nullable = false)
    private UUID receiverHubId;

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
    private Order(
        UUID receiverCompanyId,
        UUID receiverHubId,
        String deliveryRequest,
        UUID createdBy
    ) {
        // 필수 업체·허브 정보 검증
        if (receiverCompanyId == null || receiverHubId == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_REQUEST
            );
        }

        this.orderId = UUID.randomUUID();
        this.receiverCompanyId = receiverCompanyId;
        this.receiverHubId = receiverHubId;
        this.status = OrderStatus.PENDING;
        this.totalAmount = 0L;
        this.deliveryRequest = deliveryRequest;
        initializeCreatedBy(createdBy);
    }

    /**
     * 새로운 주문을 만듭니다.
     * 최초 상태는 PENDING이고 상품이 아직 없으므로 총액은 0원입니다.
     */
    public static Order create(
        UUID receiverCompanyId,
        UUID receiverHubId,
        String deliveryRequest,
        UUID createdBy
    ) {
        return new Order(
            receiverCompanyId,
            receiverHubId,
            deliveryRequest,
            createdBy
        );
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
    public void cancel(UUID canceledBy, String cancelReason) {
        // 주문과 상품 상태 사전 검사
        validateCancellation();
        // 취소 정보 검사
        if (canceledBy == null
                || cancelReason == null
                || cancelReason.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_REQUEST
            );
        }

        // 삭제·취소되지 않은 상품만 취소
        orderItems.stream()
                .filter(item -> !item.isDeleted())
                .filter(item ->
                        item.getStatus()
                                != OrderItemStatus.CANCELLED
                )
                .forEach(item ->
                        item.cancel(
                                canceledBy,
                                cancelReason
                        )
                );

        this.status = OrderStatus.CANCELLED;
        this.canceledBy = canceledBy;
        this.cancelReason = cancelReason;
    }

    /**
     * 주문 전체 취소 가능 여부 검사
     */
    public void validateCancellation() {
        // 삭제된 주문은 취소 불가
        if (isDeleted()) {
            throw new BusinessException(
                    ErrorCode.ORDER_STATUS_NOT_CHANGEABLE
            );
        }

        // 이미 취소된 주문
        if (this.status == OrderStatus.CANCELLED) {
            throw new BusinessException(
                    ErrorCode.ORDER_ALREADY_CANCELED
            );
        }

        // 완료·실패 주문은 취소 불가
        if (this.status == OrderStatus.COMPLETED
                || this.status == OrderStatus.FAILED) {
            throw new BusinessException(
                    ErrorCode.ORDER_STATUS_NOT_CHANGEABLE
            );
        }

        // 완료·실패 상품이 하나라도 있으면 전체 취소 불가
        boolean invalidItemExists = orderItems.stream()
                .filter(item -> !item.isDeleted())
                .anyMatch(item ->
                        item.getStatus()
                                == OrderItemStatus.COMPLETED
                                || item.getStatus()
                                == OrderItemStatus.FAILED
                );

        if (invalidItemExists) {
            throw new BusinessException(
                    ErrorCode.ORDER_ITEM_STATUS_NOT_CANCELED
            );
        }
    }

    /**
     * 주문 상품 개별 취소
     */
    public void cancelItem(
            UUID orderItemId,
            UUID canceledBy,
            String cancelReason
    ) {
        // 취소할 수 없는 주문 상태 검사
        if (this.status == OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CANCELED);
        }

        if (this.status == OrderStatus.COMPLETED
                || this.status == OrderStatus.FAILED) {
            throw new BusinessException(
                    ErrorCode.ORDER_STATUS_NOT_CHANGEABLE
            );
        }

        // 주문에 포함된 상품 조회
        OrderItem orderItem = getOrderItem(orderItemId);

        // 선택한 상품만 취소
        orderItem.cancel(canceledBy, cancelReason);

        // 취소 상품을 제외하고 총액 재계산
        recalculateTotalAmount();

        // 삭제되지 않은 모든 상품이 취소됐는지 확인
        boolean allActiveItemsCanceled = orderItems.stream()
                .filter(item -> !item.isDeleted())
                .allMatch(item ->
                        item.getStatus()
                                == OrderItemStatus.CANCELLED
                );

        if (allActiveItemsCanceled) {
            this.status = OrderStatus.CANCELLED;
            this.canceledBy = canceledBy;
            this.cancelReason = "모든 주문 상품이 취소되었습니다.";
        }
    }

    /**
     * 배송 요청사항 수정
     */
    public void updateDeliveryRequest(String deliveryRequest) {
        // 생성 대기 또는 배송 생성 완료 주문만 수정 가능
        boolean modifiable =
                this.status == OrderStatus.PENDING
                        || this.status == OrderStatus.CONFIRMED;

        if (!modifiable) {
            throw new BusinessException(
                    ErrorCode.ORDER_STATUS_NOT_CHANGEABLE
            );
        }

        // 요청사항 길이 방어
        if (deliveryRequest != null
                && deliveryRequest.length() > 500) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_REQUEST
            );
        }

        this.deliveryRequest = deliveryRequest;
    }

    /**
     * 담당 허브 주문인지 확인
     */
    public boolean belongsToHub(UUID hubId) {
        if (hubId == null) {
            return false;
        }

        // 수령업체 담당 허브 확인
        if (hubId.equals(this.receiverHubId)) {
            return true;
        }

        // 공급업체 담당 허브 확인
        return orderItems.stream()
                .filter(item -> !item.isDeleted())
                .anyMatch(item ->
                        hubId.equals(item.getSupplierHubId())
                );
    }

    /**
     * 사용자가 생성한 주문인지 확인
     */
    public boolean wasCreatedBy(UUID userId) {
        return userId != null
                && userId.equals(getCreatedBy());
    }

    /**
     * 모든 배송 생성 완료 처리
     */
    public void completeDeliveryCreation() {
        // 삭제·취소되지 않은 주문 상품
        List<OrderItem> activeItems = orderItems.stream()
                .filter(item ->
                        !item.isDeleted()
                                && item.getStatus()
                                != OrderItemStatus.CANCELLED
                )
                .toList();

        // 배송을 생성할 상품이 없는 경우 차단
        if (activeItems.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.ORDER_ITEM_NO_CONFIRMABLE
            );
        }

        // 모든 상품에 배송이 연결됐는지 검사
        boolean incompleteDeliveryExists = activeItems.stream()
                .anyMatch(item ->
                        item.getDeliveryId() == null
                                || item.getStatus()
                                != OrderItemStatus.DELIVERY_CREATED
                );

        if (incompleteDeliveryExists) {
            throw new BusinessException(
                    ErrorCode.INVALID_DELIVERY_RESPONSE
            );
        }

        this.status = OrderStatus.CONFIRMED;
    }

    /**
     * 주문과 주문 상품 논리 삭제
     */
    public void delete(UUID deletedBy) {
        // 삭제 사용자 검증
        if (deletedBy == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_REQUEST
            );
        }

        // 중복 삭제 방지
        if (isDeleted()) {
            throw new BusinessException(
                    ErrorCode.ORDER_ALREADY_DELETED
            );
        }

        // 취소된 주문만 삭제 가능
        if (this.status != OrderStatus.CANCELLED) {
            throw new BusinessException(
                    ErrorCode.ORDER_STATUS_NOT_DELETE
            );
        }

        // 삭제되지 않은 주문 상품만 논리 삭제
        orderItems.stream()
                .filter(item -> !item.isDeleted())
                .forEach(item ->
                        item.softDelete(deletedBy)
                );

        // 주문 논리 삭제
        softDelete(deletedBy);
    }

    /**
     * 삭제되지 않은 주문 상품 조회
     */
    public OrderItem getOrderItem(UUID orderItemId) {
        if (orderItemId == null) {
            throw new BusinessException(
                    ErrorCode.ORDER_ITEM_NOT_FOUND
            );
        }

        return orderItems.stream()
                .filter(item -> !item.isDeleted())
                .filter(item ->
                        orderItemId.equals(
                                item.getOrderItemId()
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.ORDER_ITEM_NOT_FOUND
                        )
                );
    }

    /**
     * 주문 상품 배송 완료 처리
     */
    public void completeItem(UUID orderItemId) {
        // 확정된 주문만 배송 완료 처리 가능
        if (this.status != OrderStatus.CONFIRMED) {
            throw new BusinessException(
                    ErrorCode.ORDER_STATUS_NOT_CHANGEABLE
            );
        }

        // 완료할 주문 상품 조회
        OrderItem orderItem = getOrderItem(orderItemId);

        // 선택한 주문 상품 배송 완료
        orderItem.complete();

        // 모든 활성 상품이 완료 또는 취소됐는지 확인
        boolean allItemsFinished = orderItems.stream()
                .filter(item -> !item.isDeleted())
                .allMatch(item ->
                        item.getStatus() == OrderItemStatus.COMPLETED
                                || item.getStatus() == OrderItemStatus.CANCELLED
                );

        // 모든 상품 처리가 끝나면 주문 완료
        if (allItemsFinished) {
            this.status = OrderStatus.COMPLETED;
        }
    }

    // 상품이 추가될 때마다 DB 값에 의존하지 않고 현재 상품 목록으로 총액을 계산합니다.
    private void recalculateTotalAmount() {
        this.totalAmount = orderItems.stream()
            .filter(item ->
                    item.getStatus() != OrderItemStatus.CANCELLED
            )
            .mapToLong(OrderItem::getSubtotal)
            .sum();
    }

    public UUID getOrderId() { return orderId; }
    @Override
    public UUID getId() { return orderId; }
    @Override
    public boolean isNew() { return getCreatedAt() == null; }
    public UUID getReceiverCompanyId() { return receiverCompanyId; }
    public UUID getReceiverHubId() {return receiverHubId;}
    public OrderStatus getStatus() { return status; }
    public Long getTotalAmount() { return totalAmount; }
    public String getDeliveryRequest() { return deliveryRequest; }
    public UUID getCanceledBy() { return canceledBy; }
    public String getCancelReason() { return cancelReason; }
    public List<OrderItem> getOrderItems() { return Collections.unmodifiableList(orderItems); }

    /**
     * 주문 소유 업체 확인
     */
    public boolean belongsToCompany(UUID companyId) {
        return companyId != null
                && this.receiverCompanyId.equals(companyId);
    }
}
