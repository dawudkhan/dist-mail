DROP INDEX idx_mail_task_next_retry ON mail_task;

ALTER TABLE mail_task ALTER COLUMN created_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE mail_task ALTER COLUMN updated_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE mail_task ALTER COLUMN next_retry_at DATETIMEOFFSET(6) NULL;

CREATE INDEX idx_mail_task_next_retry ON mail_task(next_retry_at);
