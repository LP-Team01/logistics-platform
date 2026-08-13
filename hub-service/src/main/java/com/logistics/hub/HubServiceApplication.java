package com.logistics.hub;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@ConfigurationPropertiesScan
@OpenAPIDefinition(servers = @Server(url = "/", description = "API Gateway"))
public class HubServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(HubServiceApplication.class, args);
    }
}
