package com.distmail.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchDispatchRequest(
    @NotEmpty List<@Valid MailRequest> mails
) {
}
