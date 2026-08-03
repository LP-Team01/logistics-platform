package com.logistics.delivery.domain.repository;

import com.logistics.delivery.domain.entity.DeliveryAgent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgent, UUID> {
}
