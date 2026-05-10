package com.distmail.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BootstrapRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapRunner.class);

    private final DispatchOrchestratorService orchestratorService;

    @Value("${distmail.bootstrap.enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${distmail.bootstrap.total-emails:10000}")
    private int totalEmails;

    public BootstrapRunner(DispatchOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @PostConstruct
    void bootstrap() {
        if (!bootstrapEnabled) return;
        var batchId = orchestratorService.publishSyntheticBatch(totalEmails);
        log.info("Bootstrap batch published: batchId={}, totalEmails={}", batchId, totalEmails);
    }
}
