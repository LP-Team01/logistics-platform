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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistics.delivery.infrastructure.client.external.naver.config.NaverApiProperties;
import com.logistics.delivery.infrastructure.client.external.naver.dto.NaverGeocodingResponseDto;
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

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = NaverGeocodingClientIntegrationTest.TestConfig.class,
    properties = {
        "naver.map.directions-base-url=http://localhost:${wiremock.server.port}",
        "naver.map.geocoding-base-url=http://localhost:${wiremock.server.port}",
        "naver.map.client-id=test-client-id",
        "naver.map.client-secret=test-client-secret",
        "eureka.client.enabled=false"
    }
)
@AutoConfigureWireMock(port = 0)
class NaverGeocodingClientIntegrationTest {

    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
    @EnableFeignClients(clients = NaverGeocodingClient.class)
    @EnableConfigurationProperties(NaverApiProperties.class)
    @Configuration
    static class TestConfig {
    }

    @Autowired
    private NaverGeocodingClient naverGeocodingClient;

    @BeforeEach
    void resetWireMock() {
        reset();
    }

    @Test
    @DisplayName("주소 지오코딩에 성공하면 좌표(x=경도, y=위도)가 파싱되고, 인증 헤더가 함께 전송된다")
    void geocodeSuccess() {
        stubFor(get(urlPathEqualTo("/map-geocode/v2/geocode"))
            .withQueryParam("query", equalTo("서울시 강남구"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"status":"OK","addresses":[
                        {"roadAddress":"도로명 주소","jibunAddress":"지번 주소","x":"127.10","y":"37.55"}
                    ],"errorMessage":""}
                    """)));

        NaverGeocodingResponseDto result = naverGeocodingClient.geocode("서울시 강남구");

        assertEquals(1, result.addresses().size());
        assertEquals("127.10", result.addresses().get(0).x());
        assertEquals("37.55", result.addresses().get(0).y());
        verify(getRequestedFor(urlPathEqualTo("/map-geocode/v2/geocode"))
            .withHeader("X-NCP-APIGW-API-KEY-ID", equalTo("test-client-id"))
            .withHeader("X-NCP-APIGW-API-KEY", equalTo("test-client-secret")));
    }

    @Test
    @DisplayName("주소를 인식하지 못하면 빈 주소 목록이 그대로 파싱된다")
    void geocodeReturnsEmptyAddressesWhenNotFound() {
        stubFor(get(urlPathEqualTo("/map-geocode/v2/geocode"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"status":"OK","addresses":[],"errorMessage":""}
                    """)));

        NaverGeocodingResponseDto result = naverGeocodingClient.geocode("존재하지 않는 주소");

        assertTrue(result.addresses().isEmpty());
    }
}