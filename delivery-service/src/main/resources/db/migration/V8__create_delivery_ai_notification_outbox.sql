CREATE TABLE p_delivery_ai_notification_outbox (
    event_id UUID PRIMARY KEY,
    delivery_id UUID NOT NULL UNIQUE,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_delivery_ai_notification_outbox_pending
    ON p_delivery_ai_notification_outbox (created_at)
    WHERE published_at IS NULL;