package com.logistics.ai.airequest.repository;

/**
 * AI 요청 통계 집계 쿼리의 조회 결과입니다.
 *
 * <p>Repository의 집계 결과를 서비스 계층으로 전달하는
 * 인터페이스 기반 Projection입니다.</p>
 */
public interface AiRequestStatisticsProjection {

    /**
     * 전체 AI 요청 건수를 반환합니다.
     */
    Long getTotalCount();

    /**
     * 처리 성공 건수를 반환합니다.
     */
    Long getSuccessCount();

    /**
     * 처리 실패 건수를 반환합니다.
     */
    Long getFailedCount();

    /**
     * 평균 AI 처리시간을 밀리초 단위로 반환합니다.
     */
    Double getAverageProcessingTimeMs();
}
