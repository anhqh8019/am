ALTER TABLE receivable_items
ADD paid_amount DECIMAL(18,2) NOT NULL
    CONSTRAINT df_receivable_items_paid_amount DEFAULT 0;

CREATE TABLE payment_allocations (
    id BIGINT IDENTITY PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id),
    receivable_item_id BIGINT NOT NULL REFERENCES receivable_items(id),
    amount DECIMAL(18,2) NOT NULL,
    CONSTRAINT ck_payment_allocation_amount CHECK (amount > 0)
);

CREATE INDEX ix_payment_allocations_payment
    ON payment_allocations(payment_id);

CREATE INDEX ix_payment_allocations_item
    ON payment_allocations(receivable_item_id);
