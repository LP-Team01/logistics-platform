package com.logistics.ai.slackmessage.repository;

import com.logistics.ai.slackmessage.entity.SlackMessage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Slack 메시지 이력의 저장과 조회를 담당하는 Repository입니다.
 */
public interface SlackMessageRepository
    extends JpaRepository<SlackMessage, UUID>,
    JpaSpecificationExecutor<SlackMessage> {

    /**
     * 논리적으로 삭제되지 않은 Slack 메시지를 ID로 조회합니다.
     *
     * @param slackMessageId Slack 메시지 식별자
     * @return Slack 메시지
     */
    Optional<SlackMessage> findBySlackMessageIdAndDeletedAtIsNull(
        UUID slackMessageId
    );

    /**
     * 특정 AI 요청과 수신자에 대한 Slack 메시지가 존재하는지 확인합니다.
     *
     * <p>같은 AI 결과가 동일 사용자에게 중복 발송되는 것을
     * 방지할 때 사용합니다.</p>
     *
     * @param aiRequestId AI 요청 식별자
     * @param recipientUserId 수신 사용자 식별자
     * @return 존재하면 true
     */
    boolean existsByAiRequestIdAndRecipientUserIdAndDeletedAtIsNull(
        UUID aiRequestId,
        UUID recipientUserId
    );

    /**
     * AI 요청에 연결된 최신 Slack 메시지를 조회합니다.
     *
     * @param aiRequestId AI 요청 식별자
     * @return 가장 최근에 생성된 Slack 메시지
     */
    Optional<SlackMessage>
    findFirstByAiRequestIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        UUID aiRequestId
    );
}
