package com.logistics.delivery.global.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// hub-service를 호출할 때 자신을 증명하는 키 - order-service 검증용 키와
// 별개로 관리해 한쪽이 유출되어도 다른 신뢰 관계까지 번지지 않도록 분리
@Validated
@ConfigurationProperties(prefix = "internal.hub-service")
public record HubInternalServiceProperties(

        @NotBlank
        String key
) {
}
