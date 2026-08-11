ALTER TABLE p_users
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN updated_by UUID,
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by UUID;

CREATE INDEX idx_users_deleted_at ON p_users (deleted_at);
