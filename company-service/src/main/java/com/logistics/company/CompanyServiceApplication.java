package com.logistics.company;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
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
public class CompanyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CompanyServiceApplication.class, args);
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
