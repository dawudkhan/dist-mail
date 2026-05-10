package com.distmail.service;

import com.distmail.domain.MailStatus;
import com.distmail.domain.MailTask;
import com.distmail.domain.MailTaskEntity;
import com.distmail.repository.MailTaskRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MailTaskPersistenceService {

    private final MailTaskRepository repository;

    public MailTaskPersistenceService(MailTaskRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void saveNewTask(MailTask task) {
        Instant now = Instant.now();
        MailTaskEntity entity = new MailTaskEntity();
        entity.setId(task.getId());
        entity.setBatchId(task.getBatchId());
        entity.setRecipient(task.getRecipient());
        entity.setSubject(task.getSubject());
        entity.setBody(task.getBody());
        entity.setPriority(task.getPriority());
        entity.setRetryCount(task.getRetryCount().get());
        entity.setMaxRetries(task.getMaxRetries());
        entity.setStatus(MailStatus.QUEUED);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setNextRetryAt(now);
        repository.save(entity);
    }

    @Transactional
    public void markSent(MailTask task) {
        repository.findById(task.getId()).ifPresent(entity -> {
            entity.setStatus(MailStatus.SENT);
            entity.setRetryCount(task.getRetryCount().get());
            entity.setErrorMessage(null);
            entity.setUpdatedAt(Instant.now());
            repository.save(entity);
        });
    }

    @Transactional
    public void markRetrying(MailTask task, String reason, Instant nextRetryAt) {
        repository.findById(task.getId()).ifPresent(entity -> {
            entity.setStatus(MailStatus.RETRYING);
            entity.setRetryCount(task.getRetryCount().get());
            entity.setErrorMessage(reason);
            entity.setUpdatedAt(Instant.now());
            entity.setNextRetryAt(nextRetryAt);
            repository.save(entity);
        });
    }

    @Transactional
    public void markFailed(MailTask task, String reason) {
        repository.findById(task.getId()).ifPresent(entity -> {
            entity.setStatus(MailStatus.FAILED);
            entity.setRetryCount(task.getRetryCount().get());
            entity.setErrorMessage(reason);
            entity.setUpdatedAt(Instant.now());
            repository.save(entity);
        });
    }
}
