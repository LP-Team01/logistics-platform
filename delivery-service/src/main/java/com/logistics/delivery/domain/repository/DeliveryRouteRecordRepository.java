package com.logistics.delivery.domain.repository;

import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryRouteRecordRepository extends JpaRepository<DeliveryRouteRecord, UUID> {
    Optional<DeliveryRouteRecord> findByDeliveryIdAndSequenceAndDeletedAtIsNull(UUID deliveryId, Integer sequence);
}
