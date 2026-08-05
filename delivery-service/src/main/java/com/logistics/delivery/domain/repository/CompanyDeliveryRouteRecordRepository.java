package com.logistics.delivery.domain.repository;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyDeliveryRouteRecordRepository extends JpaRepository<CompanyDeliveryRouteRecord, UUID> {
    Optional<CompanyDeliveryRouteRecord> findByIdAndDeliveryIdAndDeletedAtIsNull(UUID id, UUID deliveryId);
}
