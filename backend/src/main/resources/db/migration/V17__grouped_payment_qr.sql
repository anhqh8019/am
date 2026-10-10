ALTER TABLE payment_qr_requests ALTER COLUMN receivable_item_id BIGINT NULL;
ALTER TABLE payment_qr_requests ADD receivable_id BIGINT NULL REFERENCES receivables(id);

CREATE TABLE payment_qr_allocations (
    id BIGINT IDENTITY PRIMARY KEY,
    payment_qr_request_id BIGINT NOT NULL REFERENCES payment_qr_requests(id),
    receivable_item_id BIGINT NOT NULL REFERENCES receivable_items(id),
    amount DECIMAL(18,2) NOT NULL,
    CONSTRAINT ck_payment_qr_allocation_amount CHECK(amount > 0),
    CONSTRAINT uq_payment_qr_allocation UNIQUE(payment_qr_request_id,receivable_item_id)
);

-- SQL Server biên dịch cả batch trước khi cột vừa ADD được nhận diện.
-- Dùng dynamic SQL để câu lệnh được biên dịch sau khi receivable_id đã tồn tại.
EXEC(N'
    UPDATE q
       SET receivable_id = i.receivable_id
      FROM payment_qr_requests q
      JOIN receivable_items i ON i.id = q.receivable_item_id
');

EXEC(N'
    INSERT INTO payment_qr_allocations(payment_qr_request_id,receivable_item_id,amount)
    SELECT id,receivable_item_id,amount
      FROM payment_qr_requests
     WHERE receivable_item_id IS NOT NULL
');

EXEC(N'ALTER TABLE payment_qr_requests ALTER COLUMN receivable_id BIGINT NOT NULL');
CREATE INDEX ix_payment_qr_allocation_item ON payment_qr_allocations(receivable_item_id,payment_qr_request_id);
