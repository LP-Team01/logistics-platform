package com.logistics.ai.airequest.entity;

import com.logistics.ai.global.exception.BusinessException;
import com.logistics.ai.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class AiRequestTest {

    @Test
    @DisplayName("AI 요청 생성 시 기본 상태는 PENDING이다")
    void createAiRequest() {
        // when
        AiRequest aiRequest = createAiRequest(null);

        // then
        assertThat(aiRequest.getStatus())
            .isEqualTo(AiRequestStatus.PENDING);

        assertThat(aiRequest.getPreparationBufferMinutes())
            .isZero();

        assertThat(aiRequest.getPromptVersion())
            .isEqualTo("v1");

        assertThat(aiRequest.getResponse()).isNull();
        assertThat(aiRequest.getDispatchDeadline()).isNull();
        assertThat(aiRequest.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("AI 처리 성공 결과를 기록할 수 있다")
    void markSuccess() {
        // given
        AiRequest aiRequest = createAiRequest(30);
        LocalDateTime dispatchDeadline =
            LocalDateTime.of(2026, 8, 5, 9, 0);

        // when
        aiRequest.markSuccess(
            "{\"dispatchDeadline\":\"2026-08-05T09:00:00\"}",
            dispatchDeadline,
            "gemini-3.6-flash",
            1500L
        );

        // then
        assertThat(aiRequest.getStatus())
            .isEqualTo(AiRequestStatus.SUCCESS);

        assertThat(aiRequest.getDispatchDeadline())
            .isEqualTo(dispatchDeadline);

        assertThat(aiRequest.getModel())
            .isEqualTo("gemini-3.6-flash");

        assertThat(aiRequest.getProcessingTimeMs())
            .isEqualTo(1500L);

        assertThat(aiRequest.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("AI 처리 실패 결과를 기록할 수 있다")
    void markFailed() {
        // given
        AiRequest aiRequest = createAiRequest(30);

        // when
        aiRequest.markFailed(
            "Gemini API 호출 실패",
            "gemini-3.6-flash",
            1000L
        );

        // then
        assertThat(aiRequest.getStatus())
            .isEqualTo(AiRequestStatus.FAILED);

        assertThat(aiRequest.getErrorMessage())
            .isEqualTo("Gemini API 호출 실패");

        assertThat(aiRequest.getResponse()).isNull();
        assertThat(aiRequest.getDispatchDeadline()).isNull();
    }

    @Test
    @DisplayName("FAILED 상태의 AI 요청은 재처리 상태로 변경할 수 있다")
    void prepareRetry() {
        // given
        AiRequest aiRequest = createAiRequest(30);

        aiRequest.markFailed(
            "Gemini API 호출 실패",
            "gemini-3.6-flash",
            1000L
        );

        // when
        aiRequest.prepareRetry();

        // then
        assertThat(aiRequest.getStatus())
            .isEqualTo(AiRequestStatus.PENDING);

        assertThat(aiRequest.getResponse()).isNull();
        assertThat(aiRequest.getDispatchDeadline()).isNull();
        assertThat(aiRequest.getErrorMessage()).isNull();
        assertThat(aiRequest.getModel()).isNull();
        assertThat(aiRequest.getProcessingTimeMs()).isNull();
    }

    @Test
    @DisplayName("FAILED 상태가 아닌 AI 요청은 재처리할 수 없다")
    void prepareRetryNotAllowed() {
        // given
        AiRequest aiRequest = createAiRequest(30);

        // when
        BusinessException exception =
            catchThrowableOfType(
                aiRequest::prepareRetry,
                BusinessException.class
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.AI_REQUEST_RETRY_NOT_ALLOWED);
    }

    /**
     * 테스트에서 사용할 AI 요청을 생성합니다.
     */
    private AiRequest createAiRequest(
        Integer preparationBufferMinutes
    ) {
        return AiRequest.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "8월 5일 오전 9시까지 배송해주세요.",
            LocalDateTime.of(2026, 8, 5, 15, 0),
            300,
            preparationBufferMinutes,
            "최종 발송 시한을 계산해주세요."
        );
    }
}
