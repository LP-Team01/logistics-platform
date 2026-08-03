-- 감사필드(created_at/by, updated_at/by, deleted_at/by), 인덱스는 추후 V2에서 추가 예정

CREATE TABLE p_user (
    user_id      UUID PRIMARY KEY,
    username     VARCHAR(10) NOT NULL UNIQUE,
    password     VARCHAR(100) NOT NULL,
    slack_id     VARCHAR(100) NOT NULL UNIQUE,
    role         VARCHAR(20) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    hub_id       UUID,
    company_id   UUID
);
