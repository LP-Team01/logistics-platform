package com.logistics.delivery.domain.repository;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyDeliveryRouteRecordRepository extends JpaRepository<CompanyDeliveryRouteRecord, UUID> {
    Optional<CompanyDeliveryRouteRecord> findByIdAndDeliveryIdAndDeletedAtIsNull(UUID id, UUID deliveryId);

    Optional<CompanyDeliveryRouteRecord> findByDeliveryIdAndDeletedAtIsNull(UUID deliveryId);

    Optional<CompanyDeliveryRouteRecord> findFirstByDepartureHubIdAndAgentIdIsNotNullOrderByCreatedAtDesc(UUID hubId);

    // 방문 순서 재계산 대상 - WAITING만 대상 (COMPANY_MOVING/DELIVERED는 이미 진행/완료된 구간이라 재정렬하지 않음)
    List<CompanyDeliveryRouteRecord> findByAgentIdAndStatusAndDeletedAtIsNull(UUID agentId, CompanyRouteRecordStatus status);

    @Query("SELECT r FROM CompanyDeliveryRouteRecord r "
        + "WHERE r.agentId = :agentId AND r.status <> :excludedStatus AND r.deletedAt IS NULL "
        + "ORDER BY r.deliverySequence ASC NULLS LAST")
    List<CompanyDeliveryRouteRecord> findTodayRouteByAgentId(@Param("agentId") UUID agentId,
                                                              @Param("excludedStatus") CompanyRouteRecordStatus excludedStatus);
}
