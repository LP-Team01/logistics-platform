package com.logistics.ai.routenotification.dto.responsedto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Gemini가 생성한 당일 배송 경로 안내 메시지입니다.
 *
 * @param message Slack으로 발송할 배송 경로 안내 내용
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DailyRouteAiResult(
    String message
) {

    /**
     * Gemini가 필수 결과를 정상적으로 반환했는지 확인합니다.
     *
     * @return 메시지가 존재하면 true
     */
    public boolean isValid() {
        return message != null && !message.isBlank();
    }
}
