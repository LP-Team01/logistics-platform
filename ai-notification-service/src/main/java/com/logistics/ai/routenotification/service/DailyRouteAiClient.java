package com.logistics.ai.routenotification.service;

import com.logistics.ai.routenotification.dto.responsedto.DailyRouteAiResult;

/**
 * 당일 배송 경로 Slack 메시지를 생성하는
 * AI 클라이언트의 공통 인터페이스입니다.
 */
public interface DailyRouteAiClient {

    /**
     * 프롬프트를 AI 모델에 전달하여
     * 당일 배송 경로 안내 메시지를 생성합니다.
     *
     * @param prompt AI 모델에 전달할 프롬프트
     * @return AI 실행 결과
     */
    AiExecutionResult generateDailyRouteMessage(String prompt);

    /**
     * 현재 사용 중인 AI 모델 이름을 반환합니다.
     *
     * @return AI 모델 이름
     */
    String getModel();

    /**
     * AI 모델을 한 번 실행한 결과입니다.
     *
     * @param rawResponse AI 모델의 원본 응답
     * @param result 역직렬화된 경로 안내 메시지
     * @param model 호출에 사용된 모델
     * @param processingTimeMs 처리 시간(ms)
     */
    record AiExecutionResult(
        String rawResponse,
        DailyRouteAiResult result,
        String model,
        Long processingTimeMs
    ) {
    }
}
