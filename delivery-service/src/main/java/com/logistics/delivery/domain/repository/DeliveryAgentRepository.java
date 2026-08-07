package com.logistics.delivery.domain.repository;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgent, UUID>,
    JpaSpecificationExecutor<DeliveryAgent> {

    Optional<DeliveryAgent> findByIdAndDeletedAtIsNull(UUID id);

    // (agentType, hubId) 그룹 자체를 키로 잠근다(트랜잭션 스코프, 커밋/롤백 시 자동 해제).
    @Query(value = "SELECT pg_advisory_xact_lock(hashtext(:lockKey))", nativeQuery = true)
    void lockAgentGroup(@Param("lockKey") String lockKey);

    // (agentType, hubId) 그룹 전체를 잠근다. 담당자 생성 시 정원 검증 + 배송순번 채번을 원자적으로 처리
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<DeliveryAgent> findByAgentTypeAndHubIdAndDeletedAtIsNullOrderByDeliveryOrderAsc(
        AgentType agentType, UUID hubId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<DeliveryAgent> findByAgentTypeAndHubIdAndIsAvailableTrueAndDeletedAtIsNullOrderByDeliveryOrderAsc(
        AgentType agentType, UUID hubId
    );
}
