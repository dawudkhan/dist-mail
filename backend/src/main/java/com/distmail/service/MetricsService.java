package com.distmail.service;

import com.distmail.domain.DashboardSnapshot;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class MetricsService {

    private final AtomicLong submitted = new AtomicLong();
    private final AtomicLong completed = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong lastCompletedForRate = new AtomicLong();
    private final AtomicLong lastSnapshotEpochMs = new AtomicLong();
    private volatile Instant startedAt;

    public void startIfNeeded() {
        if (startedAt == null) {
            synchronized (this) {
                if (startedAt == null) {
                    startedAt = Instant.now();
                }
            }
        }
    }

    public void incrementSubmitted() { submitted.incrementAndGet(); }
    public void incrementCompleted() { completed.incrementAndGet(); }
    public void incrementFailed() { failed.incrementAndGet(); }

    public DashboardSnapshot snapshot(int activeThreads, int configuredThreads, int queueSize, int retryQueueSize) {
        startIfNeeded();
        Instant now = Instant.now();
        long elapsedSeconds = Math.max(1, now.getEpochSecond() - startedAt.getEpochSecond());
        long done = completed.get();
        long prevDone = lastCompletedForRate.getAndSet(done);
        long nowMs = now.toEpochMilli();
        long prevMs = lastSnapshotEpochMs.getAndSet(nowMs);
        long deltaMs = prevMs == 0 ? 1000 : Math.max(1, nowMs - prevMs);
        long deltaDone = Math.max(0, done - prevDone);
        double throughput = (deltaDone * 1000.0) / deltaMs;
        return new DashboardSnapshot(
            elapsedSeconds,
            activeThreads,
            configuredThreads,
            queueSize,
            retryQueueSize,
            submitted.get(),
            done,
            failed.get(),
            throughput,
            now
        );
    }
}
