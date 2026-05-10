package com.distmail.domain;

import java.util.List;

public record TelemetryResponse(
    DashboardSnapshot current,
    PressureSnapshot pressure,
    List<ThroughputPoint> trend
) {
}
