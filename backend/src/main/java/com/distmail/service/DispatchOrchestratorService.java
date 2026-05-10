package com.distmail.service;

import com.distmail.api.BatchDispatchRequest;
import com.distmail.api.MailRequest;
import com.distmail.config.DistMailProperties;
import com.distmail.domain.MailTask;
import com.distmail.kafka.KafkaProducerService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class DispatchOrchestratorService {

    private final KafkaProducerService producerService;
    private final DistMailProperties properties;

    public DispatchOrchestratorService(KafkaProducerService producerService, DistMailProperties properties) {
        this.producerService = producerService;
        this.properties = properties;
    }

    public UUID publishBatch(BatchDispatchRequest request) {
        UUID batchId = UUID.randomUUID();
        request.mails().forEach(mail -> producerService.publishTask(toTask(mail, batchId)));
        return batchId;
    }

    public UUID publishSyntheticBatch(int totalEmails) {
        UUID batchId = UUID.randomUUID();
        List<MailTask> tasks = new ArrayList<>(totalEmails);
        for (int i = 0; i < totalEmails; i++) {
            int n = i + 1;
            tasks.add(new MailTask(UUID.randomUUID(), batchId, "user" + n + "@example.com", "DIST-MAIL Notice #" + n,
                "This is distributed dispatch test message #" + n, ThreadLocalRandom.current().nextInt(1, 11),
                properties.maxRetries(), Instant.now()));
        }
        tasks.forEach(producerService::publishTask);
        return batchId;
    }

    private MailTask toTask(MailRequest request, UUID batchId) {
        return new MailTask(UUID.randomUUID(), batchId, request.recipient(), request.subject(), request.body(),
            request.priority(), request.maxRetries(), Instant.now());
    }
}
