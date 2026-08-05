package com.logistics.ai.slackmessage.entity;

/**
 * Slack으로 발송하는 메시지의 유형입니다.
 */
public enum SlackMessageType {

    /**
     * 주문 생성에 따른 배송 알림입니다.
     */
    ORDER_CREATED,

    /**
     * AI가 계산한 최종 발송 시한 알림입니다.
     */
    DISPATCH_DEADLINE,

    /**
     * Slack 연동 확인용 테스트 메시지입니다.
     */
    TEST
}
