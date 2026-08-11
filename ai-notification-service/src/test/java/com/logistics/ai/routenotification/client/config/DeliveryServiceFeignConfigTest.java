package com.logistics.ai.routenotification.client.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeliveryServiceFeignConfigTest {

    private final DeliveryServiceFeignConfig config =
        new DeliveryServiceFeignConfig();

    @Test
    @DisplayName("Delivery Service 요청에 내부 서비스 인증 헤더를 추가한다")
    void addsInternalServiceAuthenticationHeaders() {
        // given
        RequestInterceptor interceptor =
            config.deliveryServiceInternalAuthInterceptor(
                "test-internal-service-key"
            );

        RequestTemplate requestTemplate =
            new RequestTemplate();

        // when
        interceptor.apply(requestTemplate);

        // then
        assertThat(
            requestTemplate.headers()
                .get("X-Internal-Service")
        ).containsExactly(
            "ai-notification-service"
        );

        assertThat(
            requestTemplate.headers()
                .get("X-Internal-Service-Key")
        ).containsExactly(
            "test-internal-service-key"
        );
    }

    @Test
    @DisplayName("내부 서비스 키가 없으면 요청을 차단한다")
    void rejectsRequestWhenInternalServiceKeyIsMissing() {
        // given
        RequestInterceptor interceptor =
            config.deliveryServiceInternalAuthInterceptor("");

        RequestTemplate requestTemplate =
            new RequestTemplate();

        // when & then
        assertThatThrownBy(
            () -> interceptor.apply(requestTemplate)
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "INTERNAL_SERVICE_KEY가 설정되지 않았습니다."
            );
    }
}
