package com.distmail.api;

import com.distmail.domain.BatchReport;
import com.distmail.domain.DashboardSnapshot;
import com.distmail.domain.MailStatus;
import com.distmail.domain.PressureSnapshot;
import com.distmail.domain.TelemetryResponse;
import com.distmail.repository.MailTaskRepository;
import com.distmail.service.DispatchOrchestratorService;
import com.distmail.service.DispatcherService;
import com.distmail.service.TelemetryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.Map;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/dist-mail")
public class DispatchController {

    private final DispatchOrchestratorService orchestratorService;
    private final DispatcherService dispatcherService;
    private final TelemetryService telemetryService;
    private final MailTaskRepository repository;

    public DispatchController(DispatchOrchestratorService orchestratorService, DispatcherService dispatcherService,
                              TelemetryService telemetryService, MailTaskRepository repository) {
        this.orchestratorService = orchestratorService;
        this.dispatcherService = dispatcherService;
        this.telemetryService = telemetryService;
        this.repository = repository;
    }

    @PostMapping("/dispatch")
    public BatchDispatchResponse dispatch(@Valid @RequestBody BatchDispatchRequest request) {
        UUID batchId = orchestratorService.publishBatch(request);
        return new BatchDispatchResponse(batchId, request.mails().size(), dispatcherService.currentSnapshot());
    }

    @PostMapping("/dispatch/synthetic")
    public BatchDispatchResponse dispatchSynthetic(@RequestParam(defaultValue = "10000") @Min(1) int totalEmails) {
        UUID batchId = orchestratorService.publishSyntheticBatch(totalEmails);
        return new BatchDispatchResponse(batchId, totalEmails, dispatcherService.currentSnapshot());
    }

    @GetMapping("/dashboard")
    public DashboardSnapshot dashboard() { return dispatcherService.currentSnapshot(); }

    @GetMapping("/dashboard/pressure")
    public PressureSnapshot pressure() {
        DashboardSnapshot snapshot = dispatcherService.currentSnapshot();
        return telemetryService.pressure(snapshot);
    }

    @GetMapping("/dashboard/trend")
    public Map<String, Object> trend(@RequestParam(defaultValue = "60") @Min(1) int limit) {
        return Map.of("points", telemetryService.trend(limit));
    }

    @GetMapping("/dashboard/realtime")
    public TelemetryResponse realtime(@RequestParam(defaultValue = "60") @Min(1) int trendLimit) {
        DashboardSnapshot snapshot = dispatcherService.currentSnapshot();
        telemetryService.record(snapshot);
        return telemetryService.response(snapshot, trendLimit);
    }

    @GetMapping("/report/{batchId}")
    public BatchReport report(@PathVariable UUID batchId) {
        long total = repository.countByBatchId(batchId);
        long queued = repository.countByBatchIdAndStatus(batchId, MailStatus.QUEUED)
            + repository.countByBatchIdAndStatus(batchId, MailStatus.PENDING);
        long retrying = repository.countByBatchIdAndStatus(batchId, MailStatus.RETRYING);
        long sent = repository.countByBatchIdAndStatus(batchId, MailStatus.SENT);
        long failed = repository.countByBatchIdAndStatus(batchId, MailStatus.FAILED);
        long inProgress = Math.max(0, total - sent - failed);
        return new BatchReport(batchId, total, queued, retrying, sent, failed, inProgress);
    }
}
