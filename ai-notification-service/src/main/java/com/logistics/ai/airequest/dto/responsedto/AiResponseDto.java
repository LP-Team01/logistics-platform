package com.logistics.ai.airequest.dto.responsedto;

import com.logistics.ai.airequest.entity.AiRequest;
import com.logistics.ai.airequest.entity.AiRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI 요청 이력과 처리 결과를 반환하는 응답 DTO입니다.
 *
 * <p>엔티티를 API에 직접 노출하지 않고 필요한 정보만 반환합니다.</p>
 */
@Schema(description = "AI 요청 처리 결과")
public record AiResponseDto(

    @Schema(description = "AI 요청 식별자")
    UUID aiRequestId,

    @Schema(description = "Kafka 이벤트 식별자")
    UUID eventId,

    @Schema(description = "주문 식별자")
    UUID orderId,

    @Schema(description = "배송 식별자")
    UUID deliveryId,

    @Schema(description = "고객 배송 요청사항")
    String requestText,

    @Schema(description = "희망 도착 일시")
    LocalDateTime requestedArrivalAt,

    @Schema(description = "예상 배송시간(분)")
    Integer estimatedDurationMinutes,

    @Schema(description = "배송 준비시간(분)")
    Integer preparationBufferMinutes,

    @Schema(description = "Gemini에 전달한 최종 프롬프트")
    String prompt,

    @Schema(description = "Gemini 원본 응답")
    String response,

    @Schema(description = "AI가 계산한 최종 발송 시한")
    LocalDateTime dispatchDeadline,

    @Schema(
        description = "AI 요청 처리 상태",
        allowableValues = {"PENDING", "SUCCESS", "FAILED"}
    )
    AiRequestStatus status,

    @Schema(description = "사용한 Gemini 모델")
    String model,

    @Schema(description = "프롬프트 버전")
    String promptVersion,

    @Schema(description = "AI 처리시간(ms)")
    Long processingTimeMs,

    @Schema(description = "AI 처리 실패 원인")
    String errorMessage,

    @Schema(description = "생성자 식별자")
    UUID createdBy,

    @Schema(description = "생성 일시")
    LocalDateTime createdAt,

    @Schema(description = "최종 수정자 식별자")
    UUID updatedBy,

    @Schema(description = "최종 수정 일시")
    LocalDateTime updatedAt
) {

    /**
     * AiRequest 엔티티를 API 응답 DTO로 변환합니다.
     *
     * @param aiRequest 변환할 AI 요청 엔티티
     * @return 변환된 AI 요청 응답
     */
    public static AiResponseDto from(AiRequest aiRequest) {
        return new AiResponseDto(
            aiRequest.getAiRequestId(),
            aiRequest.getEventId(),
            aiRequest.getOrderId(),
            aiRequest.getDeliveryId(),
            aiRequest.getRequestText(),
            aiRequest.getRequestedArrivalAt(),
            aiRequest.getEstimatedDurationMinutes(),
            aiRequest.getPreparationBufferMinutes(),
            aiRequest.getPrompt(),
            aiRequest.getResponse(),
            aiRequest.getDispatchDeadline(),
            aiRequest.getStatus(),
            aiRequest.getModel(),
            aiRequest.getPromptVersion(),
            aiRequest.getProcessingTimeMs(),
            aiRequest.getErrorMessage(),
            aiRequest.getCreatedBy(),
            aiRequest.getCreatedAt(),
            aiRequest.getUpdatedBy(),
            aiRequest.getUpdatedAt()
        );
    }
}
