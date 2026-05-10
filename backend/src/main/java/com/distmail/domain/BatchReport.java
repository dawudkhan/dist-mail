package com.distmail.domain;

import java.util.UUID;

public record BatchReport(
    UUID batchId,
    long total,
    long queued,
    long retrying,
    long sent,
    long failed,
    long inProgress
) {
}
