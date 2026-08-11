package com.logistics.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CustomPostFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        // 내부 서비스 호출 시작 시간 기록
        long startTime = System.currentTimeMillis();

        return chain.filter(exchange)
                // 요청이 정상적으로 처리된 경우 응답 정보 기록
                .doOnSuccess(ignored -> {
                    long elapsedTime =
                            System.currentTimeMillis() - startTime;

                    log.info(
                            "requestId={}, status={}, elapsedMs={}",
                            getRequestId(exchange),
                            exchange.getResponse().getStatusCode(),
                            elapsedTime
                    );
                })
                // 요청 처리 중 예외가 발생한 경우 오류 정보 기록
                .doOnError(error -> {
                    long elapsedTime =
                            System.currentTimeMillis() - startTime;

                    log.warn(
                            "requestId={}, error={}, elapsedMs={}",
                            getRequestId(exchange),
                            error.getClass().getSimpleName(),
                            elapsedTime
                    );
                });
    }

    private String getRequestId(ServerWebExchange exchange) {
        return exchange.getRequest()
                .getHeaders()
                .getFirst(REQUEST_ID_HEADER);
    }

    @Override
    public int getOrder() {
        // 내부 서비스 응답 이후 실행
        return Ordered.LOWEST_PRECEDENCE;
    }
}
