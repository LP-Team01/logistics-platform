CREATE TABLE p_delivery_manager_approval_outbox (
    event_id UUID PRIMARY KEY,
    agent_id UUID NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempt_count INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_delivery_manager_approval_outbox_pending
    ON p_delivery_manager_approval_outbox (created_at)
    WHERE published_at IS NULL;
