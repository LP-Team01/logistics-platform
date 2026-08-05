package com.logistics.ai.slackmessage.dto.responsedto;

import com.logistics.ai.slackmessage.entity.SlackMessageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * Slack 메시지 일괄 재발송 처리 결과입니다.
 *
 * @param requestedCount 재발송 요청 건수
 * @param successCount 재발송 성공 건수
 * @param failureCount 재발송 실패 건수
 * @param results 메시지별 처리 결과
 */
@Schema(description = "Slack 메시지 일괄 재발송 결과")
public record SlackBulkRetryResponseDto(

    @Schema(description = "재발송 요청 건수", example = "3")
    int requestedCount,

    @Schema(description = "재발송 성공 건수", example = "2")
    int successCount,

    @Schema(description = "재발송 실패 건수", example = "1")
    int failureCount,

    @Schema(description = "메시지별 재발송 결과")
    List<RetryResult> results
) {

    @Schema(description = "개별 Slack 메시지 재발송 결과")
    public record RetryResult(

        @Schema(description = "Slack 메시지 식별자")
        UUID slackMessageId,

        @Schema(description = "재발송 성공 여부", example = "true")
        boolean success,

        @Schema(
            description = "재발송 이후 메시지 상태",
            example = "SENT",
            nullable = true
        )
        SlackMessageStatus status,

        @Schema(
            description = "실패 시 오류 코드",
            example = "SLACK_502",
            nullable = true
        )
        String errorCode,

        @Schema(
            description = "실패 원인",
            nullable = true
        )
        String errorMessage
    ) {
    }
}
