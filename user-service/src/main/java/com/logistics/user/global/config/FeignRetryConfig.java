package com.logistics.user.global.config;

import feign.Retryer;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * hub-service, company-service, delivery-service 호출용 공통 Feign 재시도 설정.
 * 5xx/커넥션 오류처럼 일시적인 장애만 재시도하고, 4xx는 비즈니스 실패로 보고 즉시 전파한다.
 */
@Configuration
public class FeignRetryConfig {

    @Bean
    public Retryer retryer() {
        // 100ms 시작, 최대 1s 간격, 최대 3번 시도(최초 1회 + 재시도 2회)
        return new Retryer.Default(100, 1000, 3);
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

        return (methodKey, response) -> {
            Exception decoded = defaultDecoder.decode(methodKey, response);

            if (response.status() >= 500) {
                return new RetryableException(
                    response.status(),
                    decoded.getMessage(),
                    response.request().httpMethod(),
                    decoded,
                    (Long) null,
                    response.request()
                );
            }

            return decoded;
        };
    }
}
