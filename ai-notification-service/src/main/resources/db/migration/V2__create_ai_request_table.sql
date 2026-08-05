CREATE TABLE p_ai_request
(
    ai_request_id              UUID         NOT NULL,
    event_id                   UUID         NOT NULL,
    order_id                   UUID         NOT NULL,
    delivery_id                UUID         NOT NULL,

    request_text               TEXT,
    requested_arrival_at       TIMESTAMP    NOT NULL,
    estimated_duration_minutes INTEGER      NOT NULL,
    preparation_buffer_minutes INTEGER      NOT NULL DEFAULT 0,

    prompt                     TEXT         NOT NULL,
    response                   TEXT,
    dispatch_deadline          TIMESTAMP,

    status                     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    model                      VARCHAR(100),
    prompt_version             VARCHAR(20)  NOT NULL DEFAULT 'v1',
    processing_time_ms         BIGINT,
    error_message              TEXT,

    created_at                 TIMESTAMP    NOT NULL,
    created_by                 UUID         NOT NULL,
    updated_at                 TIMESTAMP,
    updated_by                 UUID,
    deleted_at                 TIMESTAMP,
    deleted_by                 UUID,

    CONSTRAINT p_ai_request_pkey
        PRIMARY KEY (ai_request_id),

    CONSTRAINT uk_ai_request_event
        UNIQUE (event_id),

    CONSTRAINT ck_ai_request_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),

    CONSTRAINT ck_ai_request_estimated_duration
        CHECK (estimated_duration_minutes > 0),

    CONSTRAINT ck_ai_request_preparation_buffer
        CHECK (preparation_buffer_minutes >= 0),

    CONSTRAINT ck_ai_request_processing_time
        CHECK (processing_time_ms IS NULL OR processing_time_ms >= 0)
);

CREATE INDEX idx_ai_request_order
    ON p_ai_request (order_id);

CREATE INDEX idx_ai_request_delivery
    ON p_ai_request (delivery_id);

CREATE INDEX idx_ai_request_status_created
    ON p_ai_request (status, created_at DESC);
