package com.logistics.ai.slackmessage.dto.requestdto;

import com.logistics.ai.slackmessage.entity.SlackMessageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Slack 메시지 목록 검색 조건입니다.
 *
 * <p>값이 전달된 조건만 검색에 적용합니다.</p>
 *
 * @param recipientUserId 물류 시스템 사용자 식별자
 * @param status Slack 메시지 발송 상태
 */
@Schema(description = "Slack 메시지 목록 검색 조건")
public record SlackMessageSearchCondition(

    @Schema(description = "물류 시스템 사용자 식별자")
    UUID recipientUserId,

    @Schema(
        description = "Slack 메시지 발송 상태",
        example = "SENT"
    )
    SlackMessageStatus status
) {
}
