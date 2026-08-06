package com.logistics.order.query.controller;

import com.logistics.order.global.auth.HeaderRoleValidator;
import com.logistics.order.query.application.OrderQueryService;
import com.logistics.order.query.dto.OrderDetailResponse;
import com.logistics.order.query.dto.OrderSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderQueryController {

    private final OrderQueryService orderQueryService;
    private final HeaderRoleValidator roleValidator;

    /**
     * 주문 상세 조회
     */
    @Operation(
            summary = "주문 상세 조회",
            description = "주문 ID로 주문과 주문 상품 목록을 조회합니다."
    )
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(
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
            ) UUID hubId
    ) {
        roleValidator.validate(
            userRole,
            "MASTER",
            "HUB_MANAGER",
            "COMPANY_MANAGER",
            "DELIVERY_MANAGER"
        );

        return ResponseEntity.ok(
                orderQueryService.getOrderDetail(orderId,
                    userId,
                    userRole,
                    companyId,
                    hubId
                )
        );
    }

    /**
     * 주문 목록 페이징 조회
     */
    @Operation(
            summary = "주문 목록 조회",
            description = "주문 목록을 최신순으로 페이징 조회합니다."
    )
    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> getOrders(
            @Parameter(description = "검색어", example = "감자")
            @RequestParam(required = false)
            String keyword,
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = DESC)
            Pageable pageable,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader(
                    value = "X-Company-Id",
                    required = false
            ) UUID companyId,
            @RequestHeader(
                    value = "X-Hub-Id",
                    required = false
            ) UUID hubId
    ) {

        return ResponseEntity.ok(orderQueryService.getOrders(
            keyword,
            pageable,
            userId,
            userRole,
            companyId,
            hubId
        ));
    }
}
