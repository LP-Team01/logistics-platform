package com.logistics.gateway.filter;

import com.logistics.gateway.config.SecurityRuleProperties;
import com.logistics.gateway.config.UserRole;
import com.logistics.gateway.global.exception.BusinessException;
import com.logistics.gateway.global.exception.ErrorCode;
import com.logistics.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter , Ordered {

    // user-service JwtUtil의 AUTHORIZATION_KEY("auth")와 반드시 일치해야 함
    private static final String ROLE_CLAIM = "auth";
    private static final List<String> TRUSTED_HEADERS = List.of("X-User-Id", "X-Hub-Id", "X-Company-Id","X-Role");
    private static final PathPatternParser PATTERN_PARSER = new PathPatternParser();

    private final JwtUtil jwtUtil;
    private final SecurityRuleProperties securityRuleProperties;

    private List<PathPattern> permitAllPatterns;
    private List<CompiledRule> compiledRules;

    // 설정의 패턴 문자열을 기동 시점에 한 번만 PathPattern으로 컴파일해둔다
    // (요청마다 패턴을 다시 파싱하지 않기 위함).
    @PostConstruct
    private void compilePatterns() {
        this.permitAllPatterns = Optional.ofNullable(securityRuleProperties.permitAll())
            .orElseGet(List::of)
            .stream()
            .map(PATTERN_PARSER::parse)
            .toList();

        this.compiledRules = Optional.ofNullable(securityRuleProperties.rules())
            .orElseGet(List::of)
            .stream()
            .map(rule -> new CompiledRule(PATTERN_PARSER.parse(rule.path()), rule))
            .toList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

        // 클라이언트가 직접 넣어 보낸 신뢰 헤더는 무조건 제거 (스푸핑 방지)
        requestBuilder.headers(headers -> TRUSTED_HEADERS.forEach(headers::remove));

        PathContainer path = PathContainer.parsePath(exchange.getRequest().getURI().getPath());
        if (isPermitAll(path)) {
            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        }

        // JWT 유효성 검사
        String accessToken = jwtUtil.getJwtFromHeader(exchange.getRequest());
        Claims claims = jwtUtil.parseClaim(accessToken);


        String userId = claims.getSubject();
        String role = claims.get(ROLE_CLAIM, String.class);
        String method = exchange.getRequest().getMethod().name();

        if (!isAuthorized(path, method, role)) {
            return Mono.error(new BusinessException(ErrorCode.ACCESS_DENIED));
        }

        requestBuilder.header("X-User-Id", userId);
        requestBuilder.header("X-Role", role);
        // hubId/companyId는 MASTER 등 소속이 없는 유저의 토큰에는 클레임 자체가 없을 수 있음
        putIfPresent(requestBuilder, claims, "hubId", "X-Hub-Id");
        putIfPresent(requestBuilder, claims, "companyId", "X-Company-Id");

        ServerHttpRequest mutatedRequest = requestBuilder.build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPermitAll(PathContainer path) {
        return permitAllPatterns.stream().anyMatch(pattern -> pattern.matches(path));
    }

    // 매칭되는 규칙이 하나도 없으면 기본 거부. 위에서부터 순서대로 첫 매치 규칙만 적용한다.
    private boolean isAuthorized(PathContainer path, String method, String role) {
        return compiledRules.stream()
            .filter(compiled -> compiled.pattern().matches(path))
            .filter(compiled -> matchesMethod(compiled.rule(), method))
            .findFirst()
            .map(compiled -> hasRole(compiled.rule(), role))
            .orElse(false);
    }

    private boolean matchesMethod(SecurityRuleProperties.Rule rule, String method) {
        List<String> methods = rule.methods();
        return methods == null || methods.isEmpty()
            || methods.stream().anyMatch(m -> m.equalsIgnoreCase(method));
    }

    private boolean hasRole(SecurityRuleProperties.Rule rule, String role) {
        List<UserRole> roles = rule.roles();
        return roles != null && roles.stream().anyMatch(r -> r.name().equals(role));
    }

    private void putIfPresent(ServerHttpRequest.Builder builder, Claims claims, String claimName, String headerName) {
        Object value = claims.get(claimName);
        if (value != null) {
            builder.header(headerName, value.toString());
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }


    private record CompiledRule(PathPattern pattern, SecurityRuleProperties.Rule rule) {
    }
}
