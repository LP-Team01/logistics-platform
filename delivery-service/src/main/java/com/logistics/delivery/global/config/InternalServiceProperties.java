package com.logistics.delivery.global.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "internal.service")
public record InternalServiceProperties(

        @NotBlank
        String name,

        @NotBlank
        String key,

        @NotEmpty
        List<String> allowedNames
) {
}