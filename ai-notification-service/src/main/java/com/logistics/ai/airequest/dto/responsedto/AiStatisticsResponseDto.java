package com.logistics.ai.airequest.dto.responsedto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 요청 처리 통계를 반환하는 응답 DTO입니다.
 */
@Schema(description = "AI 요청 처리 통계")
public record AiStatisticsResponseDto(

    @Schema(
        description = "전체 AI 요청 건수",
        example = "10"
    )
    Long totalCount,

    @Schema(
        description = "처리 성공 건수",
        example = "8"
    )
    Long successCount,

    @Schema(
        description = "처리 실패 건수",
        example = "2"
    )
    Long failedCount,

    @Schema(
        description = "평균 AI 처리시간(ms)",
        example = "3250.75"
    )
    Double averageProcessingTimeMs
) {
}
