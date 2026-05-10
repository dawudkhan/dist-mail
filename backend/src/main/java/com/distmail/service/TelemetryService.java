package com.distmail.service;

import com.distmail.domain.DashboardSnapshot;
import com.distmail.domain.PressureSnapshot;
import com.distmail.domain.TelemetryResponse;
import com.distmail.domain.ThroughputPoint;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TelemetryService {

    private static final int MAX_POINTS = 300;
    private final Deque<ThroughputPoint> points = new ArrayDeque<>();

    public synchronized void record(DashboardSnapshot snapshot) {
        if (points.size() >= MAX_POINTS) {
            points.removeFirst();
        }
        points.addLast(new ThroughputPoint(
            snapshot.capturedAt().toEpochMilli(),
            snapshot.throughputPerSecond(),
            snapshot.queueSize(),
            snapshot.retryQueueSize(),
            snapshot.completed(),
            snapshot.permanentlyFailed()
        ));
    }

    public synchronized List<ThroughputPoint> trend(int limit) {
        int effective = Math.max(1, Math.min(limit, MAX_POINTS));
        List<ThroughputPoint> all = new ArrayList<>(points);
        if (all.size() <= effective) {
            return all;
        }
        return all.subList(all.size() - effective, all.size());
    }

    public PressureSnapshot pressure(DashboardSnapshot snapshot) {
        double queueUtil = snapshot.configuredThreads() <= 0
            ? 0
            : Math.min(1.0, snapshot.queueSize() / (double) Math.max(1, snapshot.configuredThreads() * 250));

        double retryPressure = snapshot.submitted() <= 0
            ? 0
            : Math.min(1.0, snapshot.retryQueueSize() / (double) Math.max(1, snapshot.submitted()));

        double threadUtil = snapshot.configuredThreads() <= 0
            ? 0
            : Math.min(1.0, snapshot.activeThreads() / (double) snapshot.configuredThreads());

        double score = (queueUtil * 0.55) + (retryPressure * 0.25) + (threadUtil * 0.20);
        String level = score < 0.35 ? "LOW" : score < 0.70 ? "MEDIUM" : "HIGH";

        return new PressureSnapshot(queueUtil, retryPressure, threadUtil, score, level);
    }

    public TelemetryResponse response(DashboardSnapshot snapshot, int trendLimit) {
        return new TelemetryResponse(snapshot, pressure(snapshot), trend(trendLimit));
    }
}
