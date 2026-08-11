package com.logistics.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class CustomPreFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Gateway에서 요청별 고유 추적 ID 생성
        String requestId = UUID.randomUUID().toString();

        // 내부 서비스가 동일한 요청 ID를 받을 수 있도록 헤더 추가
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> headers.remove(REQUEST_ID_HEADER))
                .header(REQUEST_ID_HEADER, requestId)
                .build();

        // 클라이언트에서도 요청 ID를 확인할 수 있도록 응답 헤더 추가
        exchange.getResponse()
                .getHeaders()
                .set(REQUEST_ID_HEADER, requestId);

        log.info(
                "requestId={}, method={}, uri={}",
                requestId,
                request.getMethod(),
                request.getURI()
        );

        return chain.filter(
                exchange.mutate()
                        .request(request)
                        .build()
        );
    }

    @Override
    public int getOrder() {
        // JWT 인증 필터보다 먼저 실행
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
