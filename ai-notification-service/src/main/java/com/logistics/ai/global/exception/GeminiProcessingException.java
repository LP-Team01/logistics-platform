package com.logistics.ai.global.exception;

/**
 * Gemini 호출 또는 응답 처리 과정에서 발생하는 예외입니다.
 */
public class GeminiProcessingException extends BusinessException {

    public GeminiProcessingException(String message) {
        super(ErrorCode.AI_RESPONSE_FAILED, message);
    }

    public GeminiProcessingException(
        String message,
        Throwable cause
    ) {
        super(ErrorCode.AI_RESPONSE_FAILED, message, cause);
    }
}
