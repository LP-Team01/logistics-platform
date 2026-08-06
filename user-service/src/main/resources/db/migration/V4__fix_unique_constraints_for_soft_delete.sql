-- REJECTED/soft-delete된 유저의 username, slack_id는 재사용 가능해야 하는데
-- 기존 UNIQUE 제약은 deleted_at/status와 무관하게 전체 행에 적용되어 재가입 시
-- DB 제약 위반이 발생할 수 있었음. 활성 유저(soft-delete 안 되고 REJECTED 아닌)에
-- 대해서만 유니크를 강제하는 partial unique index로 대체.
ALTER TABLE p_users DROP CONSTRAINT p_users_username_key;
ALTER TABLE p_users DROP CONSTRAINT p_users_slack_id_key;

CREATE UNIQUE INDEX ux_users_username_active
    ON p_users (username)
    WHERE deleted_at IS NULL AND status <> 'REJECTED';

CREATE UNIQUE INDEX ux_users_slack_id_active
    ON p_users (slack_id)
    WHERE deleted_at IS NULL AND status <> 'REJECTED';
