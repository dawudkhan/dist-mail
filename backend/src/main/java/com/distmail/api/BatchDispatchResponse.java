package com.distmail.api;

import com.distmail.domain.DashboardSnapshot;
import java.util.UUID;

public record BatchDispatchResponse(
    UUID batchId,
    int queued,
    DashboardSnapshot snapshot
) {
}
