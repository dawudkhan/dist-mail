package com.distmail.domain;

public record ThroughputPoint(
    long timestampEpochMs,
    double throughputPerSecond,
    int queueSize,
    int retryQueueSize,
    long completed,
    long permanentlyFailed
) {
}
