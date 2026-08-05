package com.logistics.ai.slackmessage.dto.responsedto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Slack 연동 테스트 메시지 발송 결과입니다.
 *
 * @param success 발송 성공 여부
 * @param recipientSlackId Slack 수신자 ID
 * @param channelId 생성된 DM 채널 ID
 * @param slackTimestamp Slack 메시지 타임스탬프
 * @param sentAt 발송 완료 시각
 */
@Schema(description = "Slack 연동 테스트 메시지 발송 결과")
public record SlackTestMessageResponseDto(

    @Schema(description = "발송 성공 여부", example = "true")
    boolean success,

    @Schema(description = "Slack 수신자 ID")
    String recipientSlackId,

    @Schema(description = "Slack DM 채널 ID", example = "D0123456789")
    String channelId,

    @Schema(description = "Slack 메시지 타임스탬프")
    String slackTimestamp,

    @Schema(description = "발송 완료 시각")
    LocalDateTime sentAt
) {
}
