package com.distmail.domain;

import java.time.Instant;

public record DashboardSnapshot(
    long elapsedSeconds,
    int activeThreads,
    int configuredThreads,
    int queueSize,
    int retryQueueSize,
    long submitted,
    long completed,
    long permanentlyFailed,
    double throughputPerSecond,
    Instant capturedAt
) {
}
