-- 논리 삭제되지 않은 AI 요청에 대해서만
-- event_id 중복을 금지합니다.
--
-- 기존 AI 요청이 soft delete되면
-- 동일한 event_id로 새 요청을 생성할 수 있습니다.

ALTER TABLE p_ai_request
DROP CONSTRAINT IF EXISTS uk_ai_request_event;

CREATE UNIQUE INDEX ux_ai_request_event_active
    ON p_ai_request (event_id)
    WHERE deleted_at IS NULL;
