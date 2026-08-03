package com.logistics.ai.common.exception;

import com.logistics.ai.airequest.entity.AiRequestStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * 재처리할 수 없는 상태의 AI 요청에 재처리를 요청한 경우 발생합니다.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class AiRequestRetryNotAllowedException extends RuntimeException {

    public AiRequestRetryNotAllowedException(
        UUID aiRequestId,
        AiRequestStatus status
    ) {
        super(
            "FAILED 상태의 AI 요청만 재처리할 수 있습니다. "
                + "aiRequestId=" + aiRequestId
                + ", status=" + status
        );
    }
}
