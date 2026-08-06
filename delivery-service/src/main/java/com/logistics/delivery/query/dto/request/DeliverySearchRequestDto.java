package com.logistics.delivery.query.dto.request;

import com.logistics.delivery.domain.entity.DeliveryStatus;
import java.util.UUID;

public record DeliverySearchRequestDto(
    DeliveryStatus status,
    UUID orderId,
    UUID orderItemId,
    UUID companyAgentId
) {
}
