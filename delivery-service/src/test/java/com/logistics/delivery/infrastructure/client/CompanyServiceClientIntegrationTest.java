package com.logistics.delivery.infrastructure.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.logistics.delivery.infrastructure.client.dto.CompanyServiceCompanyResponseDto;
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

// CompanyServiceClient(배송 생성 시 receiverCompanyId 존재 검증용 Feign Client)가 실제 HTTP 왕복을
// 올바르게 수행하는지 WireMock 스텁 서버로 검증한다.
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = CompanyServiceClientIntegrationTest.TestConfig.class,
    properties = {
        "spring.cloud.openfeign.client.config.company-service.url=http://localhost:${wiremock.server.port}",
        "eureka.client.enabled=false"
    }
)
@AutoConfigureWireMock(port = 0)
class CompanyServiceClientIntegrationTest {

    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
    @EnableFeignClients(clients = CompanyServiceClient.class)
    @Configuration
    static class TestConfig {
    }

    @Autowired
    private CompanyServiceClient companyServiceClient;

    @BeforeEach
    void resetWireMock() {
        reset();
    }

    @Test
    @DisplayName("업체 조회에 성공하면 companyId가 그대로 파싱된다")
    void getCompanySuccess() {
        UUID companyId = UUID.randomUUID();
        stubFor(get(urlPathEqualTo("/api/companies/" + companyId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"companyId":"%s","name":"테스트업체"}
                    """.formatted(companyId))));

        CompanyServiceCompanyResponseDto result = companyServiceClient.getCompany(companyId);

        assertEquals(companyId, result.companyId());
    }

    @Test
    @DisplayName("존재하지 않는 업체를 조회하면 404 FeignException이 발생한다")
    void getCompanyNotFound() {
        UUID companyId = UUID.randomUUID();
        stubFor(get(urlPathEqualTo("/api/companies/" + companyId))
            .willReturn(aResponse().withStatus(404)));

        assertThrows(FeignException.NotFound.class, () -> companyServiceClient.getCompany(companyId));
    }
}