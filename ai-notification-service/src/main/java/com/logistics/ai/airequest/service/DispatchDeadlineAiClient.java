package com.logistics.ai.airequest.service;

import com.logistics.ai.airequest.dto.responsedto.AiCalculationResult;

/**
 * 최종 발송 시한을 계산하는 AI 클라이언트의 공통 인터페이스입니다.
 *
 * <p>서비스 계층이 Gemini와 같은 특정 AI 공급자에 직접 의존하지
 * 않도록 호출 규격을 정의합니다.</p>
 */
public interface DispatchDeadlineAiClient {

    /**
     * 프롬프트를 AI 모델에 전달하여 최종 발송 시한을 계산합니다.
     *
     * @param prompt AI 모델에 전달할 최종 프롬프트
     * @return AI 실행 결과
     */
    AiExecutionResult calculateDispatchDeadline(String prompt);

    /**
     * 현재 AI 호출에 사용되는 모델 이름을 반환합니다.
     *
     * @return 설정된 AI 모델 이름
     */
    String getModel();

    /**
     * AI 모델을 한 번 실행한 결과입니다.
     *
     * @param rawResponse AI 모델의 원본 응답
     * @param calculationResult 역직렬화된 계산 결과
     * @param model 실제 호출에 사용된 모델
     * @param processingTimeMs 처리시간(ms)
     */
    record AiExecutionResult(
        String rawResponse,
        AiCalculationResult calculationResult,
        String model,
        Long processingTimeMs
    ) {
    }
}
