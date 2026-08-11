package com.logistics.ai.slackmessage.dto.requestdto;

import com.logistics.ai.slackmessage.entity.SlackMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Slack 메시지 수정 요청입니다.
 *
 * <p>PATCH 요청이므로 값이 전달된 필드만 수정합니다.</p>
 *
 * @param recipientUserId 물류 시스템 사용자 식별자
 * @param recipientSlackId Slack 회원 식별자
 * @param messageType Slack 메시지 유형
 * @param title 메시지 제목
 * @param content 메시지 내용
 */
@Schema(description = "Slack 메시지 수정 요청")
public record SlackMessageUpdateRequestDto(

    @Schema(description = "물류 시스템 사용자 식별자")
    UUID recipientUserId,

    @Pattern(
        regexp = "^[UW][A-Z0-9]+$",
        message = "Slack 회원 ID 형식이 올바르지 않습니다."
    )
    @Schema(
        description = "Slack 회원 식별자",
        example = "U0123456789"
    )
    String recipientSlackId,

    @Schema(
        description = "Slack 메시지 유형",
        example = "DISPATCH_DEADLINE"
    )
    SlackMessageType messageType,

    @Size(
        max = 100,
        message = "메시지 제목은 100자 이하여야 합니다."
    )
    @Schema(description = "Slack 메시지 제목")
    String title,

    @Schema(description = "Slack 메시지 내용")
    String content
) {
}
