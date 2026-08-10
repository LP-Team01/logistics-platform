package com.logistics.delivery.domain.repository;

import com.logistics.delivery.domain.entity.DeliveryOrderCoordination;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryOrderCoordinationRepository
        extends JpaRepository<DeliveryOrderCoordination, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO p_delivery_order_coordination (order_id, cancelled, created_at)
            VALUES (:orderId, false, NOW())
            ON CONFLICT (order_id) DO NOTHING
            """, nativeQuery = true)
    void ensureExists(@Param("orderId") UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT coordination
            FROM DeliveryOrderCoordination coordination
            WHERE coordination.orderId = :orderId
            """)
    Optional<DeliveryOrderCoordination> findByOrderIdForUpdate(
            @Param("orderId") UUID orderId
    );
}
