package com.logistics.ai.slackmessage.dto.requestdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Slack 연동 테스트 메시지 발송 요청입니다.
 *
 * @param recipientSlackId 테스트 메시지를 받을 Slack 회원 ID
 * @param content 테스트 메시지 내용
 */
@Schema(description = "Slack 연동 테스트 메시지 발송 요청")
public record SlackTestMessageRequestDto(

    @NotBlank(message = "Slack 회원 ID는 필수입니다.")
    @Pattern(
        regexp = "^[UW][A-Z0-9]+$",
        message = "Slack 회원 ID 형식이 올바르지 않습니다."
    )
    @Schema(
        description = "테스트 메시지를 받을 Slack 회원 ID",
        example = "U0123456789"
    )
    String recipientSlackId,

    @NotBlank(message = "테스트 메시지 내용은 필수입니다.")
    @Size(
        max = 4000,
        message = "Slack 메시지는 4000자 이하여야 합니다."
    )
    @Schema(
        description = "테스트 메시지 내용",
        example = "Slack 연동 테스트 메시지입니다."
    )
    String content
) {
}
