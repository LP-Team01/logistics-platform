package com.logistics.hub.global.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "internal.hub-service")
public record InternalServiceProperties(
    @NotBlank String name,
    @NotBlank String allowedName,
    @NotBlank String key
) {
}
