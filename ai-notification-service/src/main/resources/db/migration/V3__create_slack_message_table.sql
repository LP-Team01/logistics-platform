CREATE TABLE p_slack_message
(
    slack_message_id   UUID         NOT NULL,
    ai_request_id      UUID         NOT NULL,
    recipient_user_id  UUID         NOT NULL,
    recipient_slack_id VARCHAR(50)  NOT NULL,

    message_type       VARCHAR(30)  NOT NULL,
    title              VARCHAR(100) NOT NULL,
    content            TEXT         NOT NULL,

    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count         INTEGER     NOT NULL DEFAULT 0,
    slack_timestamp     VARCHAR(50),
    sent_at              TIMESTAMP,
    error_message        TEXT,

    created_at TIMESTAMP NOT NULL,
    created_by UUID      NOT NULL,
    updated_at TIMESTAMP,
    updated_by UUID,
    deleted_at TIMESTAMP,
    deleted_by UUID,

    CONSTRAINT p_slack_message_pkey
        PRIMARY KEY (slack_message_id),

    CONSTRAINT chk_slack_message_status
        CHECK (status IN ('PENDING', 'SENT', 'FAILED')),

    CONSTRAINT chk_slack_message_type
        CHECK (
            message_type IN (
                             'ORDER_CREATED',
                             'DISPATCH_DEADLINE',
                             'TEST'
                )
            ),

    CONSTRAINT chk_slack_message_retry_count
        CHECK (retry_count >= 0)
);

CREATE INDEX idx_slack_message_ai_request
    ON p_slack_message (ai_request_id);

CREATE INDEX idx_slack_message_recipient_user
    ON p_slack_message (recipient_user_id);

CREATE INDEX idx_slack_message_status_created
    ON p_slack_message (status, created_at DESC);
