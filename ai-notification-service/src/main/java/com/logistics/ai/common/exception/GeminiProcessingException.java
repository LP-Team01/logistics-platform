package com.logistics.ai.common.exception;

/**
 * Gemini 호출 또는 응답 변환 과정에서 발생하는 예외입니다.
 */
public class GeminiProcessingException extends RuntimeException {

    public GeminiProcessingException(String message) {
        super(message);
    }

    public GeminiProcessingException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}
