package com.distmail.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MailRequest(
    @Email String recipient,
    @NotBlank String subject,
    @NotBlank String body,
    @Min(1) @Max(10) int priority,
    @Min(0) @Max(10) int maxRetries
) {
}
