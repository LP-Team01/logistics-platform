package com.logistics.delivery.infrastructure.kafka;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryAiNotificationOutboxRepository extends JpaRepository<DeliveryAiNotificationOutbox, UUID> {

    List<DeliveryAiNotificationOutbox> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}