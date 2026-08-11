package com.logistics.ai.airequest.dto.responsedto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * AI가 미세 조정한 최종 방문 순서를 반환하는 응답 DTO입니다.
 */
@Schema(description = "AI 방문 순서 미세 조정 결과")
public record VisitSequenceResponseDto(

    @Schema(description = "업체배송담당자 식별자")
    UUID agentId,

    @Schema(description = "AI가 미세 조정한 방문 순서대로 나열한 업체배송경로 ID 목록")
    List<UUID> orderedRecordIds
) {
}