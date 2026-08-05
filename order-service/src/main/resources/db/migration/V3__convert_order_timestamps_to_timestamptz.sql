-- Java의 Instant와 동일하게 UTC 기준 시점을 보존하도록 시간 컬럼을 TIMESTAMPTZ로 변경합니다.
-- 기존 TIMESTAMP 값은 UTC로 저장된 값으로 간주합니다.

ALTER TABLE p_order
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE TIMESTAMPTZ USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE p_order_items
    ALTER COLUMN requested_deadline TYPE TIMESTAMPTZ USING requested_deadline AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE TIMESTAMPTZ USING deleted_at AT TIME ZONE 'UTC';
