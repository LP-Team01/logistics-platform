package com.logistics.ai;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingConnectionAutoConfiguration;
import org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiTextEmbeddingAutoConfiguration;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI 및 Slack 알림 서비스를 실행하는 Spring Boot 애플리케이션입니다.
 *
 * <p>현재는 Gemini 채팅 모델만 사용하므로
 * 임베딩과 pgvector 자동 설정을 비활성화합니다.
 * RAG 개발을 시작할 때 아래 제외 설정을 제거합니다.</p>
 */
@ConfigurationPropertiesScan
@EnableFeignClients(
    basePackages = "com.logistics.ai.routenotification.client"
)
@EnableScheduling
@SpringBootApplication(exclude = {
    GoogleGenAiEmbeddingConnectionAutoConfiguration.class,
    GoogleGenAiTextEmbeddingAutoConfiguration.class,
    PgVectorStoreAutoConfiguration.class
})
@OpenAPIDefinition(
    servers = @Server(url = "/", description = "API Gateway"),
    security = @SecurityRequirement(name = "Bearer Authentication")
)
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class AiNotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
            AiNotificationServiceApplication.class,
            args
        );
    }

    @Bean
    OpenApiCustomizer hideGatewayHeaders() {
        return openApi -> openApi.getPaths().values().stream()
            .flatMap(path -> path.readOperations().stream())
            .filter(operation -> operation.getParameters() != null)
            .forEach(operation -> operation.getParameters().removeIf(parameter ->
                "header".equals(parameter.getIn())
                    && parameter.getName().matches("(?i)X-(User|Hub|Company)-(Id|Role)")));
    }
}
