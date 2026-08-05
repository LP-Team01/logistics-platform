package com.logistics.ai.slackmessage.entity;

import com.logistics.ai.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * Slack으로 발송한 메시지와 처리 결과를 저장하는 엔티티입니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "p_slack_message",
    indexes = {
        @Index(
            name = "idx_slack_message_ai_request",
            columnList = "ai_request_id"
        ),
        @Index(
            name = "idx_slack_message_recipient_user",
            columnList = "recipient_user_id"
        ),
        @Index(
            name = "idx_slack_message_status_created",
            columnList = "status, created_at DESC"
        )
    }
)
public class SlackMessage extends BaseEntity {

    /**
     * Slack 메시지 이력 식별자입니다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
        name = "slack_message_id",
        nullable = false,
        updatable = false
    )
    private UUID slackMessageId;

    /**
     * 메시지를 생성한 AI 요청의 논리 참조 ID입니다.
     */
    @Column(name = "ai_request_id", nullable = false, updatable = false)
    private UUID aiRequestId;

    /**
     * 메시지를 수신하는 물류 시스템 사용자 ID입니다.
     */
    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    /**
     * Slack에서 사용하는 수신자 ID입니다.
     */
    @Column(name = "recipient_slack_id", nullable = false, length = 50)
    private String recipientSlackId;

    /**
     * Slack 메시지 유형입니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private SlackMessageType messageType;

    /**
     * Slack 메시지 제목입니다.
     */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /**
     * 실제 발송할 Slack 메시지 내용입니다.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Slack 메시지 처리 상태입니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ColumnDefault("'PENDING'")
    private SlackMessageStatus status;

    /**
     * 실패한 메시지를 재발송한 횟수입니다.
     */
    @Column(name = "retry_count", nullable = false)
    @ColumnDefault("0")
    private Integer retryCount;

    /**
     * Slack API가 발급한 메시지 타임스탬프입니다.
     */
    @Column(name = "slack_timestamp", length = 50)
    private String slackTimestamp;

    /**
     * Slack 메시지 발송 성공 시각입니다.
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * Slack 메시지 발송 실패 원인입니다.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    private SlackMessage(
        UUID aiRequestId,
        UUID recipientUserId,
        String recipientSlackId,
        SlackMessageType messageType,
        String title,
        String content
    ) {
        this.aiRequestId = aiRequestId;
        this.recipientUserId = recipientUserId;
        this.recipientSlackId = recipientSlackId;
        this.messageType = messageType;
        this.title = title;
        this.content = content;
        this.status = SlackMessageStatus.PENDING;
        this.retryCount = 0;
    }

    /**
     * 발송할 Slack 메시지를 생성합니다.
     */
    public static SlackMessage create(
        UUID aiRequestId,
        UUID recipientUserId,
        String recipientSlackId,
        SlackMessageType messageType,
        String title,
        String content
    ) {
        return new SlackMessage(
            aiRequestId,
            recipientUserId,
            recipientSlackId,
            messageType,
            title,
            content
        );
    }

    /**
     * Slack 메시지 발송 성공 결과를 기록합니다.
     */
    public void markSent(String slackTimestamp) {
        this.status = SlackMessageStatus.SENT;
        this.slackTimestamp = slackTimestamp;
        this.sentAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    /**
     * Slack 메시지 발송 실패 결과를 기록합니다.
     */
    public void markFailed(String errorMessage) {
        this.status = SlackMessageStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * 실패한 Slack 메시지를 재발송 대기 상태로 변경합니다.
     *
     * <p>재처리 가능 여부는 서비스 계층에서 먼저 검증합니다.</p>
     */
    public void prepareRetry() {
        this.status = SlackMessageStatus.PENDING;
        this.retryCount++;
        this.slackTimestamp = null;
        this.sentAt = null;
        this.errorMessage = null;
    }

    /**
     * 발송 전 또는 발송 실패한 메시지 내용을 변경합니다.
     *
     * <p>PATCH 요청이므로 값이 전달된 필드만 변경합니다.
     * 수정 가능 상태인지에 대한 검증은 서비스 계층에서 처리합니다.</p>
     *
     * @param recipientUserId 물류 시스템 사용자 식별자
     * @param recipientSlackId Slack 회원 식별자
     * @param messageType Slack 메시지 유형
     * @param title Slack 메시지 제목
     * @param content Slack 메시지 내용
     */
    public void update(
        UUID recipientUserId,
        String recipientSlackId,
        SlackMessageType messageType,
        String title,
        String content
    ) {
        if (recipientUserId != null) {
            this.recipientUserId = recipientUserId;
        }

        if (recipientSlackId != null && !recipientSlackId.isBlank()) {
            this.recipientSlackId = recipientSlackId;
        }

        if (messageType != null) {
            this.messageType = messageType;
        }

        if (title != null && !title.isBlank()) {
            this.title = title;
        }

        if (content != null && !content.isBlank()) {
            this.content = content;
        }
    }
}
