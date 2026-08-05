package com.logistics.ai.slackmessage.dto.responsedto;

import com.logistics.ai.slackmessage.entity.SlackMessage;
import com.logistics.ai.slackmessage.entity.SlackMessageStatus;
import com.logistics.ai.slackmessage.entity.SlackMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Slack 메시지 처리 결과 응답 DTO입니다.
 */
@Schema(description = "Slack 메시지 처리 결과")
public record SlackMessageResponseDto(

    @Schema(description = "Slack 메시지 이력 식별자")
    UUID slackMessageId,

    @Schema(description = "AI 요청 식별자")
    UUID aiRequestId,

    @Schema(description = "물류 시스템 수신 사용자 식별자")
    UUID recipientUserId,

    @Schema(description = "Slack 수신자 식별자")
    String recipientSlackId,

    @Schema(description = "Slack 메시지 유형")
    SlackMessageType messageType,

    @Schema(description = "Slack 메시지 제목")
    String title,

    @Schema(description = "Slack 메시지 내용")
    String content,

    @Schema(description = "Slack 메시지 처리 상태")
    SlackMessageStatus status,

    @Schema(description = "메시지 재발송 횟수")
    Integer retryCount,

    @Schema(description = "Slack에서 반환한 메시지 타임스탬프")
    String slackTimestamp,

    @Schema(description = "메시지 발송 성공 시각")
    LocalDateTime sentAt,

    @Schema(description = "메시지 발송 실패 원인")
    String errorMessage,

    @Schema(description = "이력 생성 시각")
    LocalDateTime createdAt,

    @Schema(description = "마지막 수정 시각")
    LocalDateTime updatedAt

) {

    /**
     * SlackMessage 엔티티를 응답 DTO로 변환합니다.
     */
    public static SlackMessageResponseDto from(
        SlackMessage slackMessage
    ) {
        return new SlackMessageResponseDto(
            slackMessage.getSlackMessageId(),
            slackMessage.getAiRequestId(),
            slackMessage.getRecipientUserId(),
            slackMessage.getRecipientSlackId(),
            slackMessage.getMessageType(),
            slackMessage.getTitle(),
            slackMessage.getContent(),
            slackMessage.getStatus(),
            slackMessage.getRetryCount(),
            slackMessage.getSlackTimestamp(),
            slackMessage.getSentAt(),
            slackMessage.getErrorMessage(),
            slackMessage.getCreatedAt(),
            slackMessage.getUpdatedAt()
        );
    }
}
