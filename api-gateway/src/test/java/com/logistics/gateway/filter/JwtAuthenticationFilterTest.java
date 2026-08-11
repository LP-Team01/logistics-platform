package com.logistics.gateway.filter;

import com.logistics.gateway.config.SecurityRuleProperties;
import com.logistics.gateway.config.UserRole;
import com.logistics.gateway.global.exception.BusinessException;
import com.logistics.gateway.global.exception.ErrorCode;
import com.logistics.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        SecurityRuleProperties properties = new SecurityRuleProperties(
                List.of("/api/auth/**", "/docs/**", "/swagger-ui/**", "/swagger-ui.html"),
                List.of(
                        new SecurityRuleProperties.Rule(
                                "/api/orders", List.of("POST"), List.of(UserRole.COMPANY_MANAGER)
                        ),
                        new SecurityRuleProperties.Rule(
                                "/api/orders/*", List.of("GET"),
                                List.of(UserRole.MASTER, UserRole.HUB_MANAGER)
                        )
                )
        );
        filter = new JwtAuthenticationFilter(jwtUtil, properties);
        ReflectionTestUtils.invokeMethod(filter, "compilePatterns");
    }

    // 정상 JWT의 사용자 및 소속 정보가 내부 서비스 헤더로 전달되는지 검증
    @Test
    void shouldForwardUserHeadersWhenJwtIsValid() {
        Claims claims = claims("user-1", "COMPANY_MANAGER");
        claims.put("hubId", "hub-1");
        claims.put("companyId", "company-1");
        when(jwtUtil.getJwtFromHeader(any())).thenReturn("token");
        when(jwtUtil.parseClaim("token")).thenReturn(claims);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/orders")
                        .header("X-User-Id", "spoofed-user")
                        .header("X-User-Role", "MASTER")
                        .header("X-Hub-Id", "spoofed-hub")
                        .header("X-Company-Id", "spoofed-company")
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, next -> {
            forwarded.set(next);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id"))
                .isEqualTo("user-1");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Role"))
                .isEqualTo("COMPANY_MANAGER");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Hub-Id"))
                .isEqualTo("hub-1");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Company-Id"))
                .isEqualTo("company-1");
    }

    // 인증 제외 경로에서 JWT 없이 통과하며 위조된 사용자 헤더가 제거되는지 검증
    @Test
    void shouldRemoveSpoofedHeadersWithoutJwtOnPermitAllPath() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login")
                        .header("X-User-Id", "spoofed-user")
                        .header("X-User-Role", "MASTER")
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, next -> {
            forwarded.set(next);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get().getRequest().getHeaders())
                .doesNotContainKeys("X-User-Id", "X-User-Role");
        verify(jwtUtil, never()).parseClaim(any());
    }

    // Swagger UI와 서비스별 API 문서가 JWT 없이 통과하는지 검증
    @ParameterizedTest
    @ValueSource(strings = {
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/docs/user/v3/api-docs",
            "/docs/order/v3/api-docs"
    })
    void shouldAllowSwaggerPathsWithoutJwt(String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(path)
        );

        filter.filter(exchange, ignored -> Mono.empty()).block();

        verify(jwtUtil, never()).parseClaim(any());
    }

    // 요청 역할이 API 허용 역할에 포함되지 않으면 접근을 거부하는지 검증
    @Test
    void shouldRejectWhenRoleIsNotAllowed() {
        when(jwtUtil.getJwtFromHeader(any())).thenReturn("token");
        when(jwtUtil.parseClaim("token")).thenReturn(claims("user-1", "COMPANY_MANAGER"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/order-1")
        );

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty()))
                .expectErrorSatisfies(this::assertAccessDenied)
                .verify();
    }

    // 보안 규칙에 등록되지 않은 API 경로를 기본적으로 거부하는지 검증
    @Test
    void shouldRejectUnregisteredApiPathByDefault() {
        when(jwtUtil.getJwtFromHeader(any())).thenReturn("token");
        when(jwtUtil.parseClaim("token")).thenReturn(claims("user-1", "MASTER"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/unknown")
        );

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty()))
                .expectErrorSatisfies(this::assertAccessDenied)
                .verify();
    }

    // MASTER는 허브 및 업체 소속 정보가 없어도 요청할 수 있는지 검증
    @Test
    void shouldAllowMasterWithoutAffiliationIds() {
        when(jwtUtil.getJwtFromHeader(any())).thenReturn("token");
        when(jwtUtil.parseClaim("token")).thenReturn(claims("master-1", "MASTER"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/order-1")
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, next -> {
            forwarded.set(next);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Role"))
                .isEqualTo("MASTER");
        assertThat(forwarded.get().getRequest().getHeaders())
                .doesNotContainKeys("X-Hub-Id", "X-Company-Id");
    }

    private Claims claims(String subject, String role) {
        Claims claims = Jwts.claims().setSubject(subject);
        claims.put("auth", role);
        return claims;
    }

    private void assertAccessDenied(Throwable error) {
        assertThat(error).isInstanceOf(BusinessException.class);
        assertThat(((BusinessException) error).getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    }
}
