package com.logistics.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // @CreatedDate와 @LastModifiedDate가 저장·수정 시각을 자동 기록하도록 활성화합니다.
}
