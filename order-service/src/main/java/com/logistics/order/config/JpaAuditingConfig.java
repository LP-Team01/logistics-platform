package com.logistics.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing(modifyOnCreate = false)
public class JpaAuditingConfig {
    // @CreatedDate와 @LastModifiedDate가 저장·수정 시각을 자동 기록하도록 활성화합니다.
    @Bean
      public AuditorAware<UUID> auditorProvider() {
          return () -> {
              ServletRequestAttributes attributes =
                  (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
              if (attributes == null) {
                  return Optional.empty();
              }

              String userId = attributes.getRequest().getHeader("X-User-Id");
              if (userId == null || userId.isBlank()) {
                  return Optional.empty();
              }

              try {
                  return Optional.of(UUID.fromString(userId));
              } catch (IllegalArgumentException e) {
                  return Optional.empty();
              }
          };
      }
}
