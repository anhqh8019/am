CREATE TABLE payments (
    id BIGINT IDENTITY PRIMARY KEY,
    receivable_id BIGINT NOT NULL REFERENCES receivables(id),
    amount DECIMAL(18,2) NOT NULL,
    payment_date DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    payment_method VARCHAR(30) NOT NULL,
    transaction_reference VARCHAR(100) NULL,
    note NVARCHAR(300) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_payment_amount CHECK (amount > 0)
);

CREATE INDEX ix_payments_receivable ON payments(receivable_id, payment_date);

CREATE UNIQUE INDEX uq_receivable_unit_period
    ON receivables(unit_id, billing_period_id)
    WHERE unit_id IS NOT NULL;

