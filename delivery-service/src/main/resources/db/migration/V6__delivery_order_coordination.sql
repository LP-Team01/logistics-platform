CREATE TABLE p_delivery_order_coordination (
    order_id UUID PRIMARY KEY,
    cancelled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
