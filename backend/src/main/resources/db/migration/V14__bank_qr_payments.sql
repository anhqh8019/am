CREATE TABLE payment_qr_requests (
    id BIGINT IDENTITY PRIMARY KEY,
    receivable_item_id BIGINT NOT NULL REFERENCES receivable_items(id),
    reference_code VARCHAR(50) NOT NULL UNIQUE,
    amount DECIMAL(18,2) NOT NULL,
    bank_code VARCHAR(20) NOT NULL,
    bank_account VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME2 NULL,
    paid_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_payment_qr_amount CHECK(amount > 0)
);
CREATE INDEX ix_payment_qr_item_status ON payment_qr_requests(receivable_item_id,status);
CREATE TABLE bank_transactions (
    id BIGINT IDENTITY PRIMARY KEY,
    provider VARCHAR(30) NOT NULL,
    provider_transaction_id VARCHAR(100) NOT NULL,
    credit_account VARCHAR(50) NULL,
    amount DECIMAL(18,2) NOT NULL,
    description NVARCHAR(500) NULL,
    transaction_time DATETIME2 NOT NULL,
    matched_qr_request_id BIGINT NULL REFERENCES payment_qr_requests(id),
    match_status VARCHAR(30) NOT NULL,
    raw_payload NVARCHAR(4000) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_bank_transaction UNIQUE(provider,provider_transaction_id),
    CONSTRAINT ck_bank_transaction_amount CHECK(amount > 0)
);
CREATE INDEX ix_bank_transaction_match ON bank_transactions(match_status,transaction_time);
