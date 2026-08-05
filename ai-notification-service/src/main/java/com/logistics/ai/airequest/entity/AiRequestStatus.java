package com.logistics.ai.airequest.entity;

/**
 * AI 요청의 처리 상태입니다.
 */
public enum AiRequestStatus {

    // AI 처리를 기다리고 있거나 현재 처리 중인 상태
    PENDING,

    // AI 응답 및 최종 발송 시한 생성에 성공한 상태
    SUCCESS,

    // Gemini 호출 또는 응답 처리에 실패한 상태
    FAILED
}
