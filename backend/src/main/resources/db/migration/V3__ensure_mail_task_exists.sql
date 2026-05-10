IF OBJECT_ID('dbo.mail_task', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.mail_task (
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
        created_at DATETIMEOFFSET(6) NOT NULL,
        updated_at DATETIMEOFFSET(6) NOT NULL,
        next_retry_at DATETIMEOFFSET(6) NULL
    );

    CREATE INDEX idx_mail_task_status ON dbo.mail_task(status);
    CREATE INDEX idx_mail_task_batch ON dbo.mail_task(batch_id);
    CREATE INDEX idx_mail_task_next_retry ON dbo.mail_task(next_retry_at);
END
