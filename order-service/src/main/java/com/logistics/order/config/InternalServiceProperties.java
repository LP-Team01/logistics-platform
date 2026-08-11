package com.logistics.order.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 내부 서비스 호출 설정
 */
@Validated
@ConfigurationProperties(prefix = "internal.service")
public record InternalServiceProperties(

        @NotBlank
        String name,

        @NotBlank
        String key
) {
}
