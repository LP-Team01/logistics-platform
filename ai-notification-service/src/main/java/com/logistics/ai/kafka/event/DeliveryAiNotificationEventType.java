package com.logistics.ai.kafka.event;

/**
 * AI 알림 서비스가 처리할 배송 이벤트 유형입니다.
 */
public enum DeliveryAiNotificationEventType {

    /**
     * 배송이 생성되어 AI 최종 발송 시한 계산이 필요한 이벤트입니다.
     */
    DELIVERY_CREATED
}
