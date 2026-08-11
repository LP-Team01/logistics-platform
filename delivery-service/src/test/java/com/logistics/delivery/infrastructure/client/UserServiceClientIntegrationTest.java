package com.logistics.delivery.infrastructure.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.logistics.delivery.infrastructure.client.dto.UserServiceUserResponseDto;
import feign.FeignException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

// UserServiceClient(배송담당자 등록 시 사용자 존재/역할/승인상태 검증용 Feign Client)가 실제 HTTP 왕복을
// 올바르게 수행하는지 WireMock 스텁 서버로 검증한다.
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = UserServiceClientIntegrationTest.TestConfig.class,
    properties = {
        "spring.cloud.openfeign.client.config.user-service.url=http://localhost:${wiremock.server.port}",
        "eureka.client.enabled=false"
    }
)
@AutoConfigureWireMock(port = 0)
class UserServiceClientIntegrationTest {

    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
    @EnableFeignClients(clients = UserServiceClient.class)
    @Configuration
    static class TestConfig {
    }

    @Autowired
    private UserServiceClient userServiceClient;

    @BeforeEach
    void resetWireMock() {
        reset();
    }

    @Test
    @DisplayName("사용자 조회에 성공하면 역할·상태가 그대로 파싱된다")
    void getUserSuccess() {
        UUID userId = UUID.randomUUID();
        stubFor(get(urlPathEqualTo("/api/users/" + userId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"userId":"%s","role":"DELIVERY_MANAGER","status":"APPROVED"}
                    """.formatted(userId))));

        UserServiceUserResponseDto result = userServiceClient.getUser(userId);

        assertEquals(userId, result.userId());
        assertEquals("DELIVERY_MANAGER", result.role().name());
        assertEquals("APPROVED", result.status().name());
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 조회하면 404 FeignException이 발생한다")
    void getUserNotFound() {
        UUID userId = UUID.randomUUID();
        stubFor(get(urlPathEqualTo("/api/users/" + userId))
            .willReturn(aResponse().withStatus(404)));

        assertThrows(FeignException.NotFound.class, () -> userServiceClient.getUser(userId));
    }
}