package com.logistics.delivery.query.controller;

import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.query.application.DeliveryQueryService;
import com.logistics.delivery.query.dto.response.DeliveryDetailResponseDto;
import com.logistics.delivery.query.dto.response.DeliveryResponseDto;
import com.logistics.delivery.query.dto.request.DeliverySearchRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deliveries")
@Tag(name = "배송", description = "배송 단건/목록 조회 API")
public class DeliveryQueryController {

    private final DeliveryQueryService deliveryQueryService;

    @GetMapping("/{deliveryId}")
    @Operation(
        summary = "배송 단건 조회",
        description = "배송 식별자로 상세 정보와 허브 간 경로 기록 목록을 조회합니다. "
            + "COMPANY 역할은 본인 업체와 관련된 배송만 조회할 수 있습니다."
    )
    public ResponseEntity<DeliveryDetailResponseDto> getDelivery(
            @Parameter(description = "요청자 역할", required = true)
            @RequestHeader("X-User-Role") UserRole userRole,
            @Parameter(description = "요청자 사용자 id", required = true)
            @RequestHeader("X-User-Id") UUID requesterId,
            @Parameter(description = "요청자 소속 허브 id")
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @Parameter(description = "요청자 소속 업체 id")
            @RequestHeader(value = "X-Company-Id", required = false) UUID requesterCompanyId,
            @Parameter(description = "배송 id", required = true)
            @PathVariable UUID deliveryId
    ) {
        DeliveryDetailResponseDto result = deliveryQueryService.getDelivery(
            userRole, requesterId, requesterHubId, requesterCompanyId, deliveryId);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @Operation(
        summary = "배송 목록 조회/검색",
        description = "상태, 주문 id, 주문상품 id, 업체 담당자 id로 배송 목록을 검색합니다. "
            + "COMPANY 역할은 본인 업체와 관련된 배송만 조회 결과에 포함됩니다."
    )
    public ResponseEntity<DeliveryResponseDto> searchDelivery(
        @Parameter(description = "요청자 역할", required = true)
        @RequestHeader("X-User-Role") UserRole userRole,
        @Parameter(description = "요청자 사용자 id", required = true)
        @RequestHeader("X-User-Id") UUID requesterId,
        @Parameter(description = "요청자 소속 허브 id")
        @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
        @Parameter(description = "요청자 소속 업체 id")
        @RequestHeader(value = "X-Company-Id", required = false) UUID requesterCompanyId,
        @ParameterObject @ModelAttribute @Valid DeliverySearchRequestDto request,
        @ParameterObject
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
        ) {
        DeliveryResponseDto result = deliveryQueryService.searchDelivery(
            userRole, requesterId, requesterHubId, requesterCompanyId, request, pageable);
        return ResponseEntity.ok(result);
    }

}
