package com.distmail.config;

import com.distmail.domain.PoolMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "distmail")
public record DistMailProperties(
    @Min(1) int threadCount,
    @Min(1) int queueCapacity,
    @Min(1) long rateWindowEmails,
    Duration rateWindow,
    double failureRate,
    @Min(0) int smtpDelayMinMs,
    @Min(1) int smtpDelayMaxMs,
    @Min(1) int maxRetries,
    Duration retryBackoff,
    PoolMode poolMode,
    @NotBlank String topic,
    @NotBlank String consumerGroup,
    @Min(1) int consumerConcurrency,
    @Min(1) int dashboardIntervalSeconds,
    @Min(1) int poisonPillCount
) {
}
