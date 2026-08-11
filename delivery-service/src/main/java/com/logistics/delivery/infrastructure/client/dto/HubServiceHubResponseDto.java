package com.logistics.delivery.infrastructure.client.dto;

import java.util.UUID;

public record HubServiceHubResponseDto(
    UUID hubId,
    String address,
    Double latitude,
    Double longitude
) {
}