CREATE TABLE notification_jobs
(
    id             VARCHAR(36) PRIMARY KEY,
    client_id      VARCHAR(100) NOT NULL,
    transaction_id VARCHAR(36),
    channel        VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    payload        TEXT,
    attempt_count  INTEGER      NOT NULL DEFAULT 0,
    sent_at        TIMESTAMPTZ,
    failed_at      TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_jobs_client_id ON notification_jobs (client_id);
CREATE INDEX idx_notification_jobs_status ON notification_jobs (status);
CREATE INDEX idx_notification_jobs_transaction_id ON notification_jobs (transaction_id);
