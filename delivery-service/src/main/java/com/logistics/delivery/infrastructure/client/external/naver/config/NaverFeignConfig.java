package com.logistics.delivery.infrastructure.client.external.naver.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

// @FeignClient(configuration = ...)로만 참조되는 클라이언트 전용 설정
public class NaverFeignConfig {

    @Bean
    public RequestInterceptor naverAuthRequestInterceptor(NaverApiProperties properties) {
        return requestTemplate -> {
            requestTemplate.header("X-NCP-APIGW-API-KEY-ID", properties.clientId());
            requestTemplate.header("X-NCP-APIGW-API-KEY", properties.clientSecret());
        };
    }
}
