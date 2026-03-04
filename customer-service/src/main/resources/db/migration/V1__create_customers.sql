CREATE TABLE customers
(
    id         VARCHAR(36) PRIMARY KEY,
    client_id  VARCHAR(100) NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    phone      VARCHAR(50),
    kyc_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    risk_tier  VARCHAR(20)  NOT NULL DEFAULT 'LOW',
    region     VARCHAR(100),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customers_client_id ON customers (client_id);
CREATE INDEX idx_customers_kyc_status ON customers (kyc_status);
