package com.distmail.domain;

import java.util.UUID;

public record MailTaskMessage(
    UUID taskId,
    UUID batchId,
    String recipient,
    String subject,
    String body,
    int priority,
    int maxRetries,
    int retryCount
) {
}
