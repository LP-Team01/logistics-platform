package com.logistics.ai.airequest.dto.requestdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

/**
 * delivery-service가 1차 계산(최근접 이웃)한 방문 순서를 AI로 미세 조정하기 위한 요청 DTO입니다.
 */
@Schema(description = "AI 방문 순서 미세 조정 요청")
public record VisitSequenceRequestDto(

    @NotNull(message = "배송담당자 ID는 필수입니다.")
    @Schema(description = "업체배송담당자 식별자")
    UUID agentId,

    @NotNull(message = "허브 ID는 필수입니다.")
    @Schema(description = "출발 허브 식별자")
    UUID hubId,

    @NotNull(message = "허브 위도는 필수입니다.")
    @Schema(description = "출발 허브 위도")
    Double hubLatitude,

    @NotNull(message = "허브 경도는 필수입니다.")
    @Schema(description = "출발 허브 경도")
    Double hubLongitude,

    @NotEmpty(message = "방문지 목록은 한 개 이상 필요합니다.")
    @Valid
    @Schema(description = "1차 순서(최근접 이웃)가 매겨진 방문지 목록")
    List<Stop> stops
) {

    /**
     * 1차 알고리즘이 확정한 순서 그대로의 방문지 정보입니다.
     */
    @Schema(description = "방문지 정보")
    public record Stop(

        @NotNull(message = "업체배송경로 ID는 필수입니다.")
        @Schema(description = "업체배송경로 식별자")
        UUID recordId,

        @NotNull(message = "수령 업체 ID는 필수입니다.")
        @Schema(description = "수령 업체 식별자")
        UUID receiverCompanyId,

        @Schema(description = "배송지 위도")
        Double latitude,

        @Schema(description = "배송지 경도")
        Double longitude,

        @Schema(description = "허브 기준 예상 거리(km)")
        Integer estimatedDistance,

        @Schema(description = "허브 기준 예상 소요시간(분)")
        Integer estimatedDuration,

        @NotNull(message = "1차 방문 순서는 필수입니다.")
        @Positive(message = "1차 방문 순서는 0보다 커야 합니다.")
        @Schema(description = "1차(최근접 이웃) 방문 순서")
        Integer sequence
    ) {
    }
}