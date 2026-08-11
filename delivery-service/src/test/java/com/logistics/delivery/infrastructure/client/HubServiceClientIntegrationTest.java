package com.logistics.delivery.infrastructure.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.logistics.delivery.infrastructure.client.dto.HubServiceHubResponseDto;
import com.logistics.delivery.infrastructure.client.dto.HubServiceRoutePathResponseDto;
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

// HubServiceClient(내부 전용 Feign Client)가 실제 HTTP 왕복(요청 경로/헤더/쿼리 파라미터 조립, 응답 역직렬화,
// 4xx -> FeignException 매핑)을 올바르게 수행하는지 WireMock 스텁 서버로 검증한다.
// Eureka/DB 없이 이 클라이언트 하나만 등록한 최소 컨텍스트를 띄우고, 클라이언트 URL을 WireMock으로 오버라이드한다.
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = HubServiceClientIntegrationTest.TestConfig.class,
    properties = {
        "spring.cloud.openfeign.client.config.hub-service.url=http://localhost:${wiremock.server.port}",
        "eureka.client.enabled=false"
    }
)
@AutoConfigureWireMock(port = 0)
class HubServiceClientIntegrationTest {

    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
    @EnableFeignClients(clients = HubServiceClient.class)
    @Configuration
    static class TestConfig {
    }

    // 같은 클래스의 테스트들이 동일한 경로를 서로 다른 응답으로 스텁하므로, WireMock 서버(스프링 컨텍스트와 함께
    // 테스트 메서드 간 재사용됨)에 이전 테스트의 스텁이 남지 않도록 매번 리셋한다
    @BeforeEach
    void resetWireMock() {
        reset();
    }

    @Autowired
    private HubServiceClient hubServiceClient;

    @Test
    @DisplayName("허브 단건 조회에 성공하면 응답 필드가 그대로 파싱된다")
    void getHubSuccess() {
        UUID hubId = UUID.randomUUID();
        stubFor(get(urlPathEqualTo("/api/hubs/" + hubId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"hubId":"%s","address":"서울시 강남구","latitude":37.5,"longitude":127.0}
                    """.formatted(hubId))));

        HubServiceHubResponseDto result = hubServiceClient.getHub(hubId);

        assertEquals(hubId, result.hubId());
        assertEquals("서울시 강남구", result.address());
        assertEquals(37.5, result.latitude());
        assertEquals(127.0, result.longitude());
    }

    @Test
    @DisplayName("존재하지 않는 허브를 조회하면 404 FeignException이 발생한다")
    void getHubNotFound() {
        UUID hubId = UUID.randomUUID();
        stubFor(get(urlPathEqualTo("/api/hubs/" + hubId))
            .willReturn(aResponse().withStatus(404)));

        assertThrows(FeignException.NotFound.class, () -> hubServiceClient.getHub(hubId));
    }

    @Test
    @DisplayName("허브 간 경로 조회 시 내부 인증 헤더가 함께 전송되고 구간 목록이 파싱된다")
    void getRoutePathSuccess() {
        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();
        stubFor(get(urlPathEqualTo("/api/hub-routes/path"))
            .withQueryParam("departureHubId", equalTo(departureHubId.toString()))
            .withQueryParam("arrivalHubId", equalTo(arrivalHubId.toString()))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"totalDistance":12.5,"totalDuration":30,"path":[
                        {"sequence":0,"departureHubId":"%s","arrivalHubId":"%s","distance":12.5,"duration":30}
                    ]}
                    """.formatted(departureHubId, arrivalHubId))));

        HubServiceRoutePathResponseDto result = hubServiceClient.getRoutePath(
            "delivery-service", "test-key", departureHubId, arrivalHubId);

        assertEquals(1, result.path().size());
        assertEquals(departureHubId, result.path().get(0).departureHubId());
        verify(getRequestedFor(urlPathEqualTo("/api/hub-routes/path"))
            .withHeader("X-Internal-Service", equalTo("delivery-service"))
            .withHeader("X-Internal-Service-Key", equalTo("test-key")));
    }

    @Test
    @DisplayName("유효하지 않은 경로 요청이면 400 FeignException이 발생한다")
    void getRoutePathBadRequest() {
        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();
        stubFor(get(urlPathEqualTo("/api/hub-routes/path"))
            .willReturn(aResponse().withStatus(400)));

        assertThrows(FeignException.BadRequest.class, () -> hubServiceClient.getRoutePath(
            "delivery-service", "test-key", departureHubId, arrivalHubId));
    }
}