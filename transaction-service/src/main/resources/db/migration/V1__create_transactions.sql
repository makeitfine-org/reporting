CREATE TABLE transactions
(
    id            VARCHAR(36) PRIMARY KEY,
    client_id     VARCHAR(100)   NOT NULL,
    product_id    VARCHAR(100)   NOT NULL,
    product_type  VARCHAR(50),
    amount        DECIMAL(19, 4) NOT NULL,
    currency      VARCHAR(3)     NOT NULL,
    status        VARCHAR(20)    NOT NULL DEFAULT 'PROCESSING',
    transacted_at TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_client_id ON transactions (client_id);
CREATE INDEX idx_transactions_status ON transactions (status);

CREATE TABLE loan_events
(
    id             VARCHAR(36) PRIMARY KEY,
    transaction_id VARCHAR(36)    NOT NULL REFERENCES transactions (id),
    loan_id        VARCHAR(100)   NOT NULL,
    client_id      VARCHAR(100)   NOT NULL,
    amount         DECIMAL(19, 4) NOT NULL,
    currency       VARCHAR(3)     NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    occurred_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_loan_events_transaction_id ON loan_events (transaction_id);
