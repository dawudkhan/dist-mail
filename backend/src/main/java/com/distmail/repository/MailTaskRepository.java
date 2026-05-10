package com.distmail.repository;

import com.distmail.domain.MailStatus;
import com.distmail.domain.MailTaskEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailTaskRepository extends JpaRepository<MailTaskEntity, UUID> {
    long countByStatus(MailStatus status);
    long countByBatchId(UUID batchId);
    long countByBatchIdAndStatus(UUID batchId, MailStatus status);
    List<MailTaskEntity> findTop500ByStatusAndNextRetryAtLessThanEqualOrderByPriorityDescUpdatedAtAsc(MailStatus status, Instant now);
}
