package com.logistics.order.outbox;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryCompensationOutboxRepository
        extends JpaRepository<DeliveryCompensationOutbox, UUID> {

    Optional<DeliveryCompensationOutbox> findByOrderId(UUID orderId);

    List<DeliveryCompensationOutbox> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
