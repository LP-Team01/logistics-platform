package com.logistics.ai.airequest.dto.responsedto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Gemini가 계산한 최종 발송 시한과 계산 근거를 담는 DTO입니다.
 *
 * <p>Gemini의 JSON 응답을 ObjectMapper를 통해
 * 이 객체로 변환하여 사용합니다.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Gemini 최종 발송 시한 계산 결과")
public record AiCalculationResult(

    /**
     * Gemini가 계산한 최종 발송 시한입니다.
     */
    @Schema(
        description = "최종 발송 시한",
        example = "2026-08-05T11:30:00"
    )
    LocalDateTime dispatchDeadline,

    /**
     * 해당 발송 시한을 계산한 근거입니다.
     */
    @Schema(
        description = "발송 시한 계산 근거",
        example = "희망 도착 시간에서 배송시간 180분과 준비시간 30분을 차감했습니다."
    )
    String reason
) {

    /**
     * Gemini가 필수 결과를 정상적으로 반환했는지 확인합니다.
     *
     * @return 발송 시한과 계산 근거가 모두 존재하면 true
     */
    public boolean isValid() {
        return dispatchDeadline != null
            && reason != null
            && !reason.isBlank();
    }
}
