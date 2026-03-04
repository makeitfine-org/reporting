CREATE TABLE bank_products
(
    id             VARCHAR(36) PRIMARY KEY,
    product_id     VARCHAR(100)  NOT NULL UNIQUE,
    name           VARCHAR(255)  NOT NULL,
    type           VARCHAR(50)   NOT NULL,
    interest_rate  DECIMAL(5, 4) NOT NULL,
    previous_rate  DECIMAL(5, 4),
    effective_date TIMESTAMPTZ,
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bank_products_product_id ON bank_products (product_id);
CREATE INDEX idx_bank_products_type ON bank_products (type);
