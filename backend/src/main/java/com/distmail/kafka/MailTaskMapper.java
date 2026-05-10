package com.distmail.kafka;

import com.distmail.domain.MailTask;
import com.distmail.domain.MailTaskMessage;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MailTaskMapper {

    public MailTaskMessage toMessage(MailTask task) {
        return new MailTaskMessage(
            task.getId(),
            task.getBatchId(),
            task.getRecipient(),
            task.getSubject(),
            task.getBody(),
            task.getPriority(),
            task.getMaxRetries(),
            task.getRetryCount().get());
    }

    public MailTask fromMessage(MailTaskMessage message) {
        MailTask task = new MailTask(
            message.taskId(),
            message.batchId(),
            message.recipient(),
            message.subject(),
            message.body(),
            message.priority(),
            message.maxRetries(),
            Instant.now());

        int retries = message.retryCount();
        for (int i = 0; i < retries; i++) {
            task.incrementRetryCount();
        }

        return task;
    }
}
