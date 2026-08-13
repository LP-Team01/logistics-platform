package com.logistics.delivery.infrastructure.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.logistics.delivery.infrastructure.client.dto.VisitSequenceRefinementRequestDto;
import com.logistics.delivery.infrastructure.client.dto.VisitSequenceRefinementResponseDto;
import feign.FeignException;
import java.util.List;
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

// ai-notification-service 내부 전용 엔드포인트 호출 - X-Internal-Service(-Key) 헤더가 함께 전송되는지도 검증한다
// (production 코드는 실패 시 404 등을 정상 케이스로 보고 NN 순서로 폴백함 - CompanyRouteSequencingService 참고)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = AiNotificationServiceClientIntegrationTest.TestConfig.class,
    properties = {
        "spring.cloud.openfeign.client.config.ai-notification-service.url=http://localhost:${wiremock.server.port}",
        "eureka.client.enabled=false"
    }
)
@AutoConfigureWireMock(port = 0)
class AiNotificationServiceClientIntegrationTest {

    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
    @EnableFeignClients(clients = AiNotificationServiceClient.class)
    @Configuration
    static class TestConfig {
    }

    @Autowired
    private AiNotificationServiceClient aiNotificationServiceClient;

    // 같은 클래스의 테스트들이 동일한 경로("/api/ai-requests/visit-sequence")를 서로 다른 응답으로 스텁하므로,
    // WireMock 서버(스프링 컨텍스트와 함께 테스트 메서드 간 재사용됨)에 이전 테스트의 스텁이 남지 않도록 매번 리셋한다
    @BeforeEach
    void resetWireMock() {
        reset();
    }

    private VisitSequenceRefinementRequestDto newRequest() {
        UUID recordId = UUID.randomUUID();
        return new VisitSequenceRefinementRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), 37.50, 127.00,
            List.of(new VisitSequenceRefinementRequestDto.Stop(
                recordId, UUID.randomUUID(), 37.51, 127.00, 5, 10, 1)));
    }

    @Test
    @DisplayName("방문 순서 미세조정 요청에 성공하면 AI가 정렬한 순서가 그대로 파싱된다")
    void refineVisitSequenceSuccess() {
        VisitSequenceRefinementRequestDto request = newRequest();
        UUID orderedId = request.stops().get(0).recordId();
        stubFor(post(urlPathEqualTo("/api/ai-requests/visit-sequence"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"agentId":"%s","orderedRecordIds":["%s"]}
                    """.formatted(request.agentId(), orderedId))));

        VisitSequenceRefinementResponseDto result =
            aiNotificationServiceClient.refineVisitSequence("delivery-service", "test-key", request);

        assertEquals(request.agentId(), result.agentId());
        assertEquals(List.of(orderedId), result.orderedRecordIds());
        verify(postRequestedFor(urlPathEqualTo("/api/ai-requests/visit-sequence"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("X-Internal-Service", equalTo("delivery-service"))
            .withHeader("X-Internal-Service-Key", equalTo("test-key")));
    }

    @Test
    @DisplayName("엔드포인트 호출이 실패(404 등)하면 FeignException.NotFound가 발생한다")
    void refineVisitSequenceNotFound() {
        stubFor(post(urlPathEqualTo("/api/ai-requests/visit-sequence"))
            .willReturn(aResponse().withStatus(404)));

        assertThrows(FeignException.NotFound.class,
            () -> aiNotificationServiceClient.refineVisitSequence("delivery-service", "test-key", newRequest()));
    }
}