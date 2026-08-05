package com.logistics.order.infrastructure.client;

import com.logistics.order.infrastructure.client.dto.CreateDeliveryRequest;
import com.logistics.order.infrastructure.client.dto.CreateDeliveryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

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
     * 주문 상품 배송 생성
     */
    @PostMapping
    CreateDeliveryResponse createDelivery(
            @RequestHeader("X-Internal-Service")
            String serviceName,

            @RequestHeader("X-Internal-Service-Key")
            String serviceKey,

            @RequestBody
            CreateDeliveryRequest request
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
}
