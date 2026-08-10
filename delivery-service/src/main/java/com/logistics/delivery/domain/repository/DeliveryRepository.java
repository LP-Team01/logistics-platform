package com.logistics.delivery.domain.repository;

import com.logistics.delivery.domain.entity.Delivery;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, UUID>, JpaSpecificationExecutor<Delivery> {
    boolean existsByOrderItemIdAndDeletedAtIsNull(UUID orderItemId);
    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID id);
    List<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);
}
