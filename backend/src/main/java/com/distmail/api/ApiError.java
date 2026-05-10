package com.distmail.api;

import java.time.Instant;

public record ApiError(String message, Instant timestamp) {
}
