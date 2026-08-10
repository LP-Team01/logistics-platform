package com.logistics.user.kafka.consumer;

import java.util.UUID;

public record DeliveryApprovalResultEvent(
    UUID userId,
    boolean success,
    String message
) {
}
