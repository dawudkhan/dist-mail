package com.distmail.service;

import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

    private final java.util.concurrent.atomic.AtomicLong sentInWindow = new java.util.concurrent.atomic.AtomicLong(0);

    public boolean tryAcquire(long maxPerWindow) {
        while (true) {
            long current = sentInWindow.get();
            if (current >= maxPerWindow) return false;
            if (sentInWindow.compareAndSet(current, current + 1)) return true;
        }
    }

    public void resetWindow() { sentInWindow.set(0); }
    public long currentCount() { return sentInWindow.get(); }
}
