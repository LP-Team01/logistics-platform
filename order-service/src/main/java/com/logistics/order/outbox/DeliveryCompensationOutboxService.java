package com.logistics.order.outbox;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryCompensationOutboxService {

    private final DeliveryCompensationOutboxRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(UUID orderId) {
        repository.findByOrderId(orderId)
            .orElseGet(() -> repository.save(new DeliveryCompensationOutbox(orderId)));
    }
}
