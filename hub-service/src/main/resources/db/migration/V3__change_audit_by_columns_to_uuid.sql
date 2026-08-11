-- 감사 필드(created_by, updated_by, deleted_by)를 String(VARCHAR) → UUID로 변경
-- AuditorAware 패턴으로 전환하면서, 값이 없을 수 있는 게 정상이므로 nullable 유지

-- 1. created_by의 NOT NULL 제약 먼저 해제
ALTER TABLE p_hubs ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE p_hub_routes ALTER COLUMN created_by DROP NOT NULL;

-- 2. 기존 "system" 문자열 제거
UPDATE p_hubs SET created_by = NULL, updated_by = NULL, deleted_by = NULL;
UPDATE p_hub_routes SET created_by = NULL, updated_by = NULL, deleted_by = NULL;

-- 3. VARCHAR → UUID 타입 변경
ALTER TABLE p_hubs
    ALTER COLUMN created_by TYPE UUID USING created_by::uuid,
    ALTER COLUMN updated_by TYPE UUID USING updated_by::uuid,
    ALTER COLUMN deleted_by TYPE UUID USING deleted_by::uuid;

ALTER TABLE p_hub_routes
    ALTER COLUMN created_by TYPE UUID USING created_by::uuid,
    ALTER COLUMN updated_by TYPE UUID USING updated_by::uuid,
    ALTER COLUMN deleted_by TYPE UUID USING deleted_by::uuid;
