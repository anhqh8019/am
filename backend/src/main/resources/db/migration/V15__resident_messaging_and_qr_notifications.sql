ALTER TABLE customers ADD zalo_user_id VARCHAR(100) NULL;
ALTER TABLE customers ADD telegram_chat_id VARCHAR(100) NULL;

CREATE TABLE payment_notifications (
    id BIGINT IDENTITY PRIMARY KEY,
    payment_qr_request_id BIGINT NOT NULL REFERENCES payment_qr_requests(id),
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_message_id VARCHAR(150) NULL,
    error_message NVARCHAR(500) NULL,
    sent_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);
CREATE INDEX ix_payment_notification_qr ON payment_notifications(payment_qr_request_id,created_at);
CREATE INDEX ix_payment_notification_status ON payment_notifications(status,created_at);
