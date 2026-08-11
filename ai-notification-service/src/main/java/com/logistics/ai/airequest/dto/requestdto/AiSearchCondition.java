package com.logistics.ai.airequest.dto.requestdto;

import com.logistics.ai.airequest.entity.AiRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * AI 요청 목록 조회에 사용하는 검색조건 DTO입니다.
 *
 * <p>모든 검색조건은 선택사항이며, 전달된 조건만 조회 쿼리에 적용합니다.</p>
 */
@Schema(description = "AI 요청 목록 검색조건")
public record AiSearchCondition(

    /**
     * 특정 주문의 AI 요청만 조회합니다.
     */
    @Schema(
        description = "주문 식별자",
        example = "550e8400-e29b-41d4-a716-446655440001"
    )
    UUID orderId,

    /**
     * 특정 배송의 AI 요청만 조회합니다.
     */
    @Schema(
        description = "배송 식별자",
        example = "550e8400-e29b-41d4-a716-446655440002"
    )
    UUID deliveryId,

    /**
     * 특정 처리 상태의 AI 요청만 조회합니다.
     */
    @Schema(
        description = "AI 처리 상태",
        allowableValues = {"PENDING", "SUCCESS", "FAILED"}
    )
    AiRequestStatus status
) {
}
