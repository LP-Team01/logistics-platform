package com.logistics.ai.airequest.dto.responsedto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * Gemini가 미세 조정한 방문 순서와 근거를 담는 DTO입니다.
 *
 * <p>Gemini의 JSON 응답을 ObjectMapper를 통해 이 객체로 변환하여 사용합니다.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Gemini 방문 순서 미세 조정 결과")
public record VisitSequenceAiResult(

    @Schema(description = "미세 조정된 방문 순서(업체배송경로 ID 목록)")
    List<UUID> orderedRecordIds,

    @Schema(
        description = "방문 순서를 조정한 근거",
        example = "허브에서 가까운 두 업체의 방문 순서를 맞바꿔 총 이동 거리를 줄였습니다."
    )
    String reason
) {

    /**
     * Gemini가 필수 결과를 정상적으로 반환했는지 확인합니다.
     *
     * @return 방문 순서와 근거가 모두 존재하면 true
     */
    public boolean isValid() {
        return orderedRecordIds != null
            && !orderedRecordIds.isEmpty()
            && reason != null
            && !reason.isBlank();
    }
}