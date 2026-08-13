package com.logistics.delivery.infrastructure.client.dto;

import java.io.Serializable;
import java.util.UUID;

// Redis 캐시(HubQueryService.getHub())가 기본 JDK 직렬화(RedisSerializer.java())를 쓰기 때문에 Serializable 필수
public record HubServiceHubResponseDto(
    UUID hubId,
    String address,
    Double latitude,
    Double longitude
) implements Serializable {
}