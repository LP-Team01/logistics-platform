package com.logistics.ai.slackmessage.service;

/**
 * Slack 메시지 발송 기능의 공통 인터페이스입니다.
 *
 * <p>서비스 계층이 Slack Java SDK 구현에 직접 의존하지 않도록
 * 메시지 발송 규격을 정의합니다.</p>
 */
public interface SlackNotificationClient {

    /**
     * 지정된 Slack 사용자에게 메시지를 발송합니다.
     *
     * @param recipientSlackId Slack 사용자 식별자
     * @param title 메시지 제목
     * @param content 메시지 내용
     * @return Slack API 발송 결과
     */
    SlackSendResult sendMessage(
        String recipientSlackId,
        String title,
        String content
    );

    /**
     * Slack 메시지 발송 결과입니다.
     *
     * @param slackTimestamp Slack 메시지 식별 타임스탬프
     * @param channelId Slack이 반환한 대화 채널 식별자
     */
    record SlackSendResult(
        String slackTimestamp,
        String channelId
    ) {
    }
}
