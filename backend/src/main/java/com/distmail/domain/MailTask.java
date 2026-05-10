package com.distmail.domain;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class MailTask implements Comparable<MailTask> {

    public static final MailTask POISON_PILL = new MailTask(new UUID(0L, 0L), new UUID(0L, 0L), "", "", "", 0, 0, Instant.EPOCH);

    private final UUID id;
    private final UUID batchId;
    private final String recipient;
    private final String subject;
    private final String body;
    private final int priority;
    private final int maxRetries;
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private volatile MailStatus status = MailStatus.PENDING;
    private volatile Instant nextRetryAt;

    public MailTask(UUID id, UUID batchId, String recipient, String subject, String body, int priority, int maxRetries, Instant createdAt) {
        this.id = id;
        this.batchId = batchId;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.priority = priority;
        this.maxRetries = maxRetries;
        this.nextRetryAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getBatchId() { return batchId; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public int getPriority() { return priority; }
    public int getMaxRetries() { return maxRetries; }
    public AtomicInteger getRetryCount() { return retryCount; }
    public MailStatus getStatus() { return status; }
    public Instant getNextRetryAt() { return nextRetryAt; }

    public boolean isPoisonPill() { return this == POISON_PILL || this.id.equals(POISON_PILL.id); }
    public boolean canRetry() { return retryCount.get() < maxRetries; }
    public int incrementRetryCount() { return retryCount.incrementAndGet(); }
    public void markQueued() { this.status = MailStatus.QUEUED; }
    public void markRetrying(Instant retryAt) { this.status = MailStatus.RETRYING; this.nextRetryAt = retryAt; }
    public void markSent() { this.status = MailStatus.SENT; }
    public void markFailed() { this.status = MailStatus.FAILED; }

    @Override
    public int compareTo(MailTask other) {
        int byPriority = Integer.compare(other.priority, this.priority);
        if (byPriority != 0) return byPriority;
        return this.nextRetryAt.compareTo(other.nextRetryAt);
    }
}
