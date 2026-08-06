-- 논리 삭제되지 않은 Slack 메시지에 대해서만
-- 동일한 AI 요청과 수신자의 메시지 중복을 방지합니다.

CREATE UNIQUE INDEX ux_slack_message_ai_request_recipient_active
    ON p_slack_message (ai_request_id, recipient_user_id)
    WHERE deleted_at IS NULL;
