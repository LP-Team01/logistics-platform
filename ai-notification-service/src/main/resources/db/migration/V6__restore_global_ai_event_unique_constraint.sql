-- Kafka event_id는 논리 삭제 여부와 관계없이
-- 전체 AI 요청 이력에서 영구적으로 유일해야 합니다.

DROP INDEX IF EXISTS ux_ai_request_event_active;

ALTER TABLE p_ai_request
    ADD CONSTRAINT uk_ai_request_event
        UNIQUE (event_id);
