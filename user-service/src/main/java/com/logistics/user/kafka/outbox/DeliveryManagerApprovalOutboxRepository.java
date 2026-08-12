package com.logistics.user.kafka.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryManagerApprovalOutboxRepository extends JpaRepository<DeliveryManagerApprovalOutbox, UUID> {

    List<DeliveryManagerApprovalOutbox> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
