package com.logistics.ai.routenotification.client;

import com.logistics.ai.routenotification.client.config.DeliveryServiceFeignConfig;
import com.logistics.ai.routenotification.dto.responsedto.DeliveryAgentPageResponseDto;
import com.logistics.ai.routenotification.dto.responsedto.TodayRouteResponseDto;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * AI 서비스에서 Delivery Service의
 * 배송 담당자 및 당일 경로 API를 호출합니다.
 */
@FeignClient(
    name = "delivery-service",
    path = "/api/delivery-agents",
    configuration = DeliveryServiceFeignConfig.class
)
public interface DeliveryServiceClient {

    /**
     * 알림을 받을 수 있는 업체 배송 담당자 목록을 조회합니다.
     */
    @GetMapping
    DeliveryAgentPageResponseDto getCompanyDeliveryAgents(
        @RequestParam("agentType") String agentType,
        @RequestParam("isAvailable") boolean isAvailable,
        @RequestParam("page") int page,
        @RequestParam("size") int size
    );

    /**
     * 배송 담당자의 오늘 방문 경로를 조회합니다.
     */
    @GetMapping("/{agentId}/today-route")
    TodayRouteResponseDto getTodayRoute(
        @PathVariable("agentId") UUID agentId
    );
}
