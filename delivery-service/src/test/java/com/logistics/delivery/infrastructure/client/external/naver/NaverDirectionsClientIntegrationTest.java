package com.logistics.delivery.infrastructure.client.external.naver;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.logistics.delivery.infrastructure.client.external.naver.config.NaverApiProperties;
import com.logistics.delivery.infrastructure.client.external.naver.dto.NaverDirectionsResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

// NaverDirectionsClient는 @FeignClient(url = "${naver.map.directions-base-url}")로 URL을 직접 지정하므로,
// 그 프로퍼티 자체를 WireMock 주소로 오버라이드한다(내부 서비스 Feign Client처럼 client-config.url 오버라이드가 아님).
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = NaverDirectionsClientIntegrationTest.TestConfig.class,
    properties = {
        "naver.map.directions-base-url=http://localhost:${wiremock.server.port}",
        "naver.map.geocoding-base-url=http://localhost:${wiremock.server.port}",
        "naver.map.client-id=test-client-id",
        "naver.map.client-secret=test-client-secret",
        "eureka.client.enabled=false"
    }
)
@AutoConfigureWireMock(port = 0)
class NaverDirectionsClientIntegrationTest {

    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
    @EnableFeignClients(clients = NaverDirectionsClient.class)
    @EnableConfigurationProperties(NaverApiProperties.class)
    @Configuration
    static class TestConfig {
    }

    @Autowired
    private NaverDirectionsClient naverDirectionsClient;

    @BeforeEach
    void resetWireMock() {
        reset();
    }

    @Test
    @DisplayName("경로 조회에 성공하면 요약 정보가 파싱되고, 인증 헤더가 함께 전송된다")
    void getDirectionsSuccess() {
        stubFor(get(urlPathEqualTo("/map-direction/v1/driving"))
            .withQueryParam("start", equalTo("127.0,37.5"))
            .withQueryParam("goal", equalTo("127.1,37.6"))
            .withQueryParam("option", equalTo("trafast"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"code":0,"message":"OK","route":{"trafast":[{"summary":{"distance":12500,"duration":1800000}}]}}
                    """)));

        NaverDirectionsResponseDto result =
            naverDirectionsClient.getDirections("127.0,37.5", "127.1,37.6", null, "trafast");

        assertEquals(12_500L, result.route().get("trafast").get(0).summary().distance());
        assertEquals(1_800_000L, result.route().get("trafast").get(0).summary().duration());
        verify(getRequestedFor(urlPathEqualTo("/map-direction/v1/driving"))
            .withHeader("X-NCP-APIGW-API-KEY-ID", equalTo("test-client-id"))
            .withHeader("X-NCP-APIGW-API-KEY", equalTo("test-client-secret")));
    }

    @Test
    @DisplayName("경유지가 있으면 waypoints 쿼리 파라미터로 함께 전송된다")
    void getDirectionsWithWaypoints() {
        stubFor(get(urlPathEqualTo("/map-direction/v1/driving"))
            .withQueryParam("waypoints", equalTo("127.05,37.55"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"code":0,"message":"OK","route":{"trafast":[{"summary":{"distance":100,"duration":100}}]}}
                    """)));

        naverDirectionsClient.getDirections("127.0,37.5", "127.1,37.6", "127.05,37.55", "trafast");

        verify(getRequestedFor(urlPathEqualTo("/map-direction/v1/driving"))
            .withQueryParam("waypoints", equalTo("127.05,37.55")));
    }
}