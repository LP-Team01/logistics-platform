package com.logistics.ai.routenotification.client.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * AI 서비스에서 Delivery Service를 호출할 때
 * 내부 서비스 인증 헤더를 추가하는 Feign 설정입니다.
 */
public class DeliveryServiceFeignConfig {

    private static final String INTERNAL_SERVICE_HEADER =
        "X-Internal-Service";

    private static final String INTERNAL_SERVICE_KEY_HEADER =
        "X-Internal-Service-Key";

    private static final String AI_NOTIFICATION_SERVICE =
        "ai-notification-service";

    /**
     * Delivery Service로 보내는 모든 Feign 요청에
     * 내부 서비스 인증 헤더를 추가합니다.
     */
    @Bean
    public RequestInterceptor deliveryServiceInternalAuthInterceptor(
        @Value("${internal.service.key:}") String internalServiceKey
    ) {
        return requestTemplate -> {
            if (!StringUtils.hasText(internalServiceKey)) {
                throw new IllegalStateException(
                    "INTERNAL_SERVICE_KEY가 설정되지 않았습니다."
                );
            }

            requestTemplate.header(
                INTERNAL_SERVICE_HEADER,
                AI_NOTIFICATION_SERVICE
            );

            requestTemplate.header(
                INTERNAL_SERVICE_KEY_HEADER,
                internalServiceKey
            );
        };
    }
}
