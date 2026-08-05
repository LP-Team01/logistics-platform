package com.logistics.order.domain.entity;

public enum OrderItemStatus {
    // 상품 주문 처리 대기
    PENDING,
    // 공급 업체가 상품 주문을 확인함
    CONFIRMED,
    // Delivery Service에 배송 생성이 완료됨
    DELIVERY_CREATED,
    // 상품 배송까지 모두 완료됨
    COMPLETED,
    // 해당 상품 주문이 취소됨
    CANCELLED,
    // 해당 상품 처리에 실패함
    FAILED
}
