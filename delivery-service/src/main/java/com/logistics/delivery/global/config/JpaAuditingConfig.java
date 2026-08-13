package com.logistics.delivery.global.config;

import com.logistics.delivery.global.common.AuditorContext;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String userId = attributes.getRequest().getHeader("X-User-Id");
                if (userId != null && !userId.isBlank()) {
                    try {
                        return Optional.of(UUID.fromString(userId));
                    } catch (IllegalArgumentException e) {
                        return Optional.empty();
                    }
                }
            }

            // HTTP 요청 컨텍스트가 없는 경로(Kafka 컨슈머 등)는 AuditorContext에 명시적으로 지정된 값을 사용
            return AuditorContext.get();
        };
    }
}
