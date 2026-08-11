package com.logistics.delivery.infrastructure.client.external.naver.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "naver.map")
public record NaverApiProperties(

    @NotBlank
    String directionsBaseUrl,

    @NotBlank
    String geocodingBaseUrl,

    String clientId,

    String clientSecret
) {
}
