package com.logistics.delivery.infrastructure.kafka;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryAgentApprovalOutboxRepository extends JpaRepository<DeliveryAgentApprovalOutbox, UUID> {

    List<DeliveryAgentApprovalOutbox> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}