package com.logistics.order.domain.repository;

import com.logistics.order.domain.entity.Order;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    /**
     * 삭제되지 않은 주문 조회
     */
    Optional<Order> findByOrderIdAndDeletedAtIsNull(UUID orderId);

}
