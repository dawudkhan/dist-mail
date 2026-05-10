package com.distmail.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mail_task", indexes = {
    @Index(name = "idx_mail_task_status", columnList = "status"),
    @Index(name = "idx_mail_task_batch", columnList = "batch_id"),
    @Index(name = "idx_mail_task_next_retry", columnList = "next_retry_at")
})
public class MailTaskEntity {

    @Id
    private UUID id;
    @Column(name = "batch_id", nullable = false)
    private UUID batchId;
    @Column(name = "recipient", nullable = false, length = 256)
    private String recipient;
    @Column(name = "subject", nullable = false, length = 256)
    private String subject;
    @Column(name = "body", nullable = false, length = 4000)
    private String body;
    @Column(name = "priority", nullable = false)
    private int priority;
    @Column(name = "retry_count", nullable = false)
    private int retryCount;
    @Column(name = "max_retries", nullable = false)
    private int maxRetries;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MailStatus status;
    @Column(name = "error_message", length = 512)
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    public MailTaskEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public MailStatus getStatus() { return status; }
    public void setStatus(MailStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }
}
