ALTER TABLE payment_qr_requests ADD
    vietqr_qr_code VARCHAR(2048) NULL,
    vietqr_qr_link VARCHAR(1000) NULL,
    vietqr_transaction_ref_id VARCHAR(500) NULL;
