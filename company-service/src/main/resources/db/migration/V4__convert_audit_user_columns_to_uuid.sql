-- 감사 컬럼을 X-User-Id와 동일한 UUID 타입으로 통일합니다.
ALTER TABLE p_companies
    ALTER COLUMN created_by TYPE UUID USING NULLIF(TRIM(created_by::TEXT), '')::UUID,
    ALTER COLUMN updated_by TYPE UUID USING NULLIF(TRIM(updated_by::TEXT), '')::UUID,
    ALTER COLUMN deleted_by TYPE UUID USING NULLIF(TRIM(deleted_by::TEXT), '')::UUID;

ALTER TABLE p_products
    ALTER COLUMN created_by TYPE UUID USING NULLIF(TRIM(created_by::TEXT), '')::UUID,
    ALTER COLUMN updated_by TYPE UUID USING NULLIF(TRIM(updated_by::TEXT), '')::UUID,
    ALTER COLUMN deleted_by TYPE UUID USING NULLIF(TRIM(deleted_by::TEXT), '')::UUID;
