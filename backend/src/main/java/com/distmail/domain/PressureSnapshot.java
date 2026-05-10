package com.distmail.domain;

public record PressureSnapshot(
    double queueUtilization,
    double retryPressure,
    double threadUtilization,
    double pressureScore,
    String level
) {
}
