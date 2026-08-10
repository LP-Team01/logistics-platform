CREATE TABLE p_delivery_compensation_outbox (
    event_id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_delivery_compensation_outbox_pending
    ON p_delivery_compensation_outbox (created_at)
    WHERE published_at IS NULL;
