package com.logistics.ai.slackmessage.dto.requestdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * 실패한 Slack 메시지 일괄 재발송 요청입니다.
 *
 * @param slackMessageIds 재발송할 Slack 메시지 식별자 목록
 */
@Schema(description = "Slack 메시지 일괄 재발송 요청")
public record SlackBulkRetryRequestDto(

    @NotEmpty(message = "재발송할 Slack 메시지 ID가 필요합니다.")
    @Size(
        max = 100,
        message = "한 번에 최대 100개의 메시지만 재발송할 수 있습니다."
    )
    @Schema(
        description = "재발송할 Slack 메시지 ID 목록",
        example = """
            [
              "1bfd8afd-d522-41b5-81d2-81387e8a08df"
            ]
            """
    )
    List<@NotNull(message = "Slack 메시지 ID는 NULL일 수 없습니다.") UUID>
    slackMessageIds
) {
}
