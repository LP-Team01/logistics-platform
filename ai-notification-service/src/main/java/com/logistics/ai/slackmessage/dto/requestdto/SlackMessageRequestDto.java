package com.logistics.ai.slackmessage.dto.requestdto;

import com.logistics.ai.slackmessage.entity.SlackMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Slack 메시지 발송 요청 DTO입니다.
 *
 * @param aiRequestId AI 요청 식별자
 * @param recipientUserId 물류 시스템 수신 사용자 식별자
 * @param recipientSlackId Slack 수신자 식별자
 * @param messageType Slack 메시지 유형
 * @param title Slack 메시지 제목
 * @param content Slack 메시지 내용
 */
@Schema(description = "Slack 메시지 발송 요청")
public record SlackMessageRequestDto(

    @NotNull(message = "AI 요청 ID는 필수입니다.")
    @Schema(
        description = "AI 요청 식별자",
        example = "cf871ec1-e9f4-46a2-aa83-748ca29e2772"
    )
    UUID aiRequestId,

    @NotNull(message = "수신 사용자 ID는 필수입니다.")
    @Schema(
        description = "물류 시스템 수신 사용자 식별자",
        example = "1b5dc6c1-e963-454d-a211-e837d159f387"
    )
    UUID recipientUserId,

    @NotBlank(message = "Slack 수신자 ID는 필수입니다.")
    @Size(max = 50, message = "Slack 수신자 ID는 50자를 초과할 수 없습니다.")
    @Schema(
        description = "Slack에서 사용하는 수신자 ID",
        example = "U0123456789"
    )
    String recipientSlackId,

    @NotNull(message = "메시지 유형은 필수입니다.")
    @Schema(
        description = "Slack 메시지 유형",
        example = "DISPATCH_DEADLINE"
    )
    SlackMessageType messageType,

    @NotBlank(message = "메시지 제목은 필수입니다.")
    @Size(max = 100, message = "메시지 제목은 100자를 초과할 수 없습니다.")
    @Schema(
        description = "Slack 메시지 제목",
        example = "최종 발송 시한 안내"
    )
    String title,

    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Schema(
        description = "Slack으로 발송할 메시지 내용",
        example = "주문 상품의 최종 발송 시한은 8월 5일 오전 9시입니다."
    )
    String content

) {
}
