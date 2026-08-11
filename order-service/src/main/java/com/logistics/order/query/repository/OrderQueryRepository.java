package com.logistics.order.query.repository;

import com.logistics.order.domain.entity.Order;
import com.logistics.order.query.dto.OrderSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/** DTO 직접 조회처럼 읽기 전용 쿼리를 정의합니다. */
public interface OrderQueryRepository {
    /**
     * 삭제되지 않은 주문과 주문 상품을 함께 조회합니다.
     */
    Optional<Order> findOrderDetail(UUID orderId);

    /**
     * 삭제되지 않은 주문 목록을 페이징하여 조회합니다.
     */
    Page<OrderSummaryResponse> findOrders(String keyword, UUID receiverCompanyId, UUID createdBy, UUID hubId, Pageable pageable);
}
