package com.logistics.delivery.query.dto.request;

import com.logistics.delivery.domain.entity.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "배송 목록 검색 조건")
public record DeliverySearchRequestDto(
    @Schema(description = "배송 상태 필터")
    DeliveryStatus status,
    @Schema(description = "주문 id 필터")
    UUID orderId,
    @Schema(description = "주문아이템 id 필터")
    UUID orderItemId,
    @Schema(description = "업체 배송 담당자 id 필터")
    UUID companyAgentId
) {
}
