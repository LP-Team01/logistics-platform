package com.logistics.ai.slackmessage.entity;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlackMessageTest {

    @Test
    @DisplayName("Slack 메시지 생성 시 기본 상태는 PENDING이다")
    void createSlackMessage() {
        // when
        SlackMessage slackMessage = createTestSlackMessage();

        // then
        assertThat(slackMessage.getStatus())
            .isEqualTo(SlackMessageStatus.PENDING);

        assertThat(slackMessage.getRetryCount())
            .isZero();

        assertThat(slackMessage.getRecipientSlackId())
            .isEqualTo("U0123456789");

        assertThat(slackMessage.getSlackTimestamp()).isNull();
        assertThat(slackMessage.getSentAt()).isNull();
        assertThat(slackMessage.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Slack 메시지 발송 성공 결과를 기록할 수 있다")
    void markSent() {
        // given
        SlackMessage slackMessage = createTestSlackMessage();

        // 이전 실패 정보가 제거되는지도 함께 확인합니다.
        slackMessage.markFailed("Slack API 호출 실패");

        // when
        slackMessage.markSent("1722844800.123456");

        // then
        assertThat(slackMessage.getStatus())
            .isEqualTo(SlackMessageStatus.SENT);

        assertThat(slackMessage.getSlackTimestamp())
            .isEqualTo("1722844800.123456");

        assertThat(slackMessage.getSentAt()).isNotNull();
        assertThat(slackMessage.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Slack 메시지 발송 실패 결과를 기록할 수 있다")
    void markFailed() {
        // given
        SlackMessage slackMessage = createTestSlackMessage();

        // when
        slackMessage.markFailed("Slack 사용자 조회 실패");

        // then
        assertThat(slackMessage.getStatus())
            .isEqualTo(SlackMessageStatus.FAILED);

        assertThat(slackMessage.getErrorMessage())
            .isEqualTo("Slack 사용자 조회 실패");
    }

    @Test
    @DisplayName("실패한 Slack 메시지를 재발송 대기 상태로 변경할 수 있다")
    void prepareRetry() {
        // given
        SlackMessage slackMessage = createTestSlackMessage();
        slackMessage.markFailed("Slack API 호출 실패");

        // when
        slackMessage.prepareRetry();

        // then
        assertThat(slackMessage.getStatus())
            .isEqualTo(SlackMessageStatus.PENDING);

        assertThat(slackMessage.getRetryCount())
            .isEqualTo(1);

        assertThat(slackMessage.getSlackTimestamp()).isNull();
        assertThat(slackMessage.getSentAt()).isNull();
        assertThat(slackMessage.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("발송 전 Slack 메시지 내용을 수정할 수 있다")
    void updateSlackMessage() {
        // given
        SlackMessage slackMessage = createTestSlackMessage();

        UUID changedRecipientUserId = UUID.randomUUID();

        // when
        slackMessage.update(
            changedRecipientUserId,
            "U9876543210",
            SlackMessageType.DISPATCH_DEADLINE,
            "변경된 발송 시한 안내",
            "최종 발송 시한이 변경되었습니다."
        );

        // then
        assertThat(slackMessage.getRecipientUserId())
            .isEqualTo(changedRecipientUserId);

        assertThat(slackMessage.getRecipientSlackId())
            .isEqualTo("U9876543210");

        assertThat(slackMessage.getMessageType())
            .isEqualTo(SlackMessageType.DISPATCH_DEADLINE);

        assertThat(slackMessage.getTitle())
            .isEqualTo("변경된 발송 시한 안내");

        assertThat(slackMessage.getContent())
            .isEqualTo("최종 발송 시한이 변경되었습니다.");
    }

    /**
     * 테스트에서 사용할 Slack 메시지를 생성합니다.
     */
    private SlackMessage createTestSlackMessage() {
        return SlackMessage.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "U0123456789",
            SlackMessageType.DISPATCH_DEADLINE,
            "최종 발송 시한 안내",
            "최종 발송 시한은 8월 5일 오전 9시입니다."
        );
    }
}
