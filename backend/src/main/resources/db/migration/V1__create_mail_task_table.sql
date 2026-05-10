CREATE TABLE mail_task (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    batch_id UNIQUEIDENTIFIER NOT NULL,
    recipient NVARCHAR(256) NOT NULL,
    subject NVARCHAR(256) NOT NULL,
    body NVARCHAR(4000) NOT NULL,
    priority INT NOT NULL,
    retry_count INT NOT NULL,
    max_retries INT NOT NULL,
    status NVARCHAR(20) NOT NULL,
    error_message NVARCHAR(512) NULL,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    next_retry_at DATETIME2 NULL
);

CREATE INDEX idx_mail_task_status ON mail_task(status);
CREATE INDEX idx_mail_task_batch ON mail_task(batch_id);
CREATE INDEX idx_mail_task_next_retry ON mail_task(next_retry_at);
