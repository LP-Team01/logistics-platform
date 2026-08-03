package com.logistics.ai.common.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * AI 요청 이력을 찾을 수 없을 때 발생하는 예외입니다.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AiRequestNotFoundException extends RuntimeException {

    /**
     * 조회하지 못한 AI 요청 식별자를 포함하여 예외를 생성합니다.
     *
     * @param aiRequestId AI 요청 식별자
     */
    public AiRequestNotFoundException(UUID aiRequestId) {
        super(
            "AI 요청 이력을 찾을 수 없습니다. aiRequestId="
                + aiRequestId
        );
    }
}
