package com.logistics.order.domain.entity;

public enum OrderStatus {
    // 주문 접수 후 처리 대기
    PENDING,
    // 업체가 주문을 확인함
    CONFIRMED,
    // 주문에 포함된 모든 처리가 완료됨
    COMPLETED,
    // 주문이 취소됨
    CANCELLED,
    // 주문 처리 중 복구할 수 없는 오류가 발생함
    FAILED
}
