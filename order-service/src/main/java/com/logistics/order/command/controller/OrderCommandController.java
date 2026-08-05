package com.logistics.order.command.controller;

import com.logistics.order.command.application.OrderCommandService;
import com.logistics.order.command.dto.CancelOrderRequest;
import com.logistics.order.command.dto.CreateOrderRequest;
import com.logistics.order.command.dto.OrderCommandResponse;

import com.logistics.order.command.dto.UpdateOrderRequest;
import com.logistics.order.global.auth.HeaderRoleValidator;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderCommandController {

    private final OrderCommandService orderCommandService;
    private final HeaderRoleValidator roleValidator;

    /**
     * 주문을 생성합니다.
     *
     * X-User-Id는 API Gateway가 JWT 검증 후 전달하는 사용자 UUID입니다.
     */
    @PostMapping
    public ResponseEntity<OrderCommandResponse> createOrder(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderCommandResponse response = orderCommandService.createOrder(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * 주문 상품 전체 취소
     */
    @Operation(
            summary = "주문 상품 전체 취소",
            description = "주문과 주문 상품을 모두 취소합니다."
    )
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderCommandResponse> cancelOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader(
                    value = "X-Company-Id",
                    required = false
            ) UUID companyId,
            @RequestHeader(
                    value = "X-Hub-Id",
                    required = false
            ) UUID hubId,
            @Valid @RequestBody CancelOrderRequest request
    ) {

        // 접근 가능한 역할 검사
        roleValidator.validate(
                userRole,
                "MASTER",
                "HUB_MANAGER",
                "COMPANY_MANAGER"
        );

        return ResponseEntity.ok(
                orderCommandService.cancelOrder(
                        orderId,
                        request,
                        userId,
                        userRole,
                        companyId,
                        hubId
                )
        );
    }

    /**
     * 주문 상품 개별 취소
     */
    @Operation(
        summary = "주문 상품 개별 취소",
        description = "주문에 포함된 상품 하나를 취소합니다."
    )
    @PatchMapping("/{orderId}/items/{orderItemId}/cancel")
    public ResponseEntity<OrderCommandResponse> cancelOrderItem(
        @PathVariable UUID orderId,
        @PathVariable UUID orderItemId,
        @RequestHeader("X-User-Id") UUID userId,
        @RequestHeader("X-User-Role") String userRole,
        @RequestHeader(
                    value = "X-Company-Id",
                    required = false
            ) UUID companyId,
         @RequestHeader(
                value = "X-Hub-Id",
                required = false
        ) UUID hubId,
        @Valid @RequestBody CancelOrderRequest request
    ) {

        // 접근 가능한 역할 검사
        roleValidator.validate(
            userRole,
            "MASTER",
            "HUB_MANAGER",
            "COMPANY_MANAGER"
        );

        return ResponseEntity.ok(
            orderCommandService.cancelOrderItem(
                orderId,
                orderItemId,
                request,
                userId,
                userRole,
                companyId,
                hubId
            )
        );
    }

    /**
     * 주문 배송 요청사항 수정
     * 상품과 수량은 재고 및 금액에 영향을 주므로 일반 수정에 포함하지 않고,
     * 기존 상품 취소 후 다시 주문하는 방식으로 처리하는 게 안전하여
     * 주문 수정은 배송 요청사항만 수정
     */
    @Operation(
            summary = "주문 수정",
            description = "대기 중인 주문의 배송 요청사항을 수정합니다."
    )
    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderCommandResponse> updateOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader(
                    value = "X-Hub-Id",
                    required = false
            ) UUID hubId,
            @Valid @RequestBody UpdateOrderRequest request
    ) {
        // 주문 수정 권한 검사
        roleValidator.validate(
                userRole,
                "MASTER",
                "HUB_MANAGER"
        );

        return ResponseEntity.ok(
                orderCommandService.updateOrder(
                        orderId,
                        request,
                        userRole,
                        hubId
                )
        );
    }

    /**
     * 주문 확정
     */
//    @Operation(
//            summary = "주문 확정",
//            description = "대기 중인 주문과 유효한 주문 상품을 확정합니다."
//    )
//    @PatchMapping("/{orderId}/confirm")
//    public ResponseEntity<OrderCommandResponse> confirmOrder(
//            @PathVariable UUID orderId,
//            @RequestHeader("X-User-Id") UUID userId,
//            @RequestHeader("X-User-Role") String userRole,
//            @RequestHeader(
//                    value = "X-Hub-Id",
//                    required = false
//            ) UUID hubId
//    ) {
//        // 주문 확정 권한 검사
//        roleValidator.validate(
//                userRole,
//                "HUB_MANAGER",
//                "MASTER"
//        );
//
//        return ResponseEntity.ok(
//                orderCommandService.confirmOrder(
//                    orderId,
//                    userRole,
//                    hubId
//                )
//        );
//    }
}
