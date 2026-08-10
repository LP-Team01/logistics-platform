package com.logistics.order.infrastructure.client;

import com.logistics.order.infrastructure.client.dto.CreateDeliveryRequest;
import com.logistics.order.infrastructure.client.dto.CreateDeliveryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Delivery Service 배송 생성
 */
@FeignClient(
        name = "delivery-service",
        path = "/api/deliveries"
)
public interface DeliveryClient {

    /**
     * 주문 상품들의 배송을 한 번의 내부 요청으로 생성합니다.
     */
    @PostMapping("/batch")
    List<CreateDeliveryResponse> createDeliveries(
            @RequestHeader("X-Internal-Service")
            String serviceName,

            @RequestHeader("X-Internal-Service-Key")
            String serviceKey,

            @RequestBody
            List<CreateDeliveryRequest> requests
    );

    /**
     * 주문 상품 배송 생성 취소
     */
    @DeleteMapping("/order-items/{orderItemId}")
    void cancelDeliveryByOrderItemId(
            @RequestHeader("X-Internal-Service")
            String serviceName,

            @RequestHeader("X-Internal-Service-Key")
            String serviceKey,

            @PathVariable UUID orderItemId
    );

    /**
     * 주문에 생성된 모든 배송을 한 번에 취소합니다.
     */
    @DeleteMapping("/orders/{orderId}")
    void cancelDeliveriesByOrderId(
            @RequestHeader("X-Internal-Service")
            String serviceName,

            @RequestHeader("X-Internal-Service-Key")
            String serviceKey,

            @PathVariable UUID orderId
    );

}
