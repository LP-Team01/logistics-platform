-- 감사 필드의 시간 타입을 Instant와 대응되는 TIMESTAMPTZ로 통일합니다.
--
-- 기존 TIMESTAMP 값은 한국 표준시(Asia/Seoul)로 기록된 값으로 간주하여
-- 동일한 실제 시점을 나타내는 TIMESTAMPTZ 값으로 변환합니다.
--
-- requested_arrival_at, dispatch_deadline, sent_at은
-- 업무상 지역 일시이므로 이번 마이그레이션 대상에서 제외합니다.

ALTER TABLE p_ai_request
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
        USING created_at AT TIME ZONE 'Asia/Seoul',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ
        USING updated_at AT TIME ZONE 'Asia/Seoul',
    ALTER COLUMN deleted_at TYPE TIMESTAMPTZ
        USING deleted_at AT TIME ZONE 'Asia/Seoul';

ALTER TABLE p_slack_message
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
        USING created_at AT TIME ZONE 'Asia/Seoul',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ
        USING updated_at AT TIME ZONE 'Asia/Seoul',
    ALTER COLUMN deleted_at TYPE TIMESTAMPTZ
        USING deleted_at AT TIME ZONE 'Asia/Seoul';
