package it.univ.lifeplanner.push.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PushSubscriptionRequest(
    @NotBlank @Size(max = 1024) String endpoint,
    @Valid @NotNull PushSubscriptionKeys keys,
    @Size(max = 512) String userAgent
) {
    public record PushSubscriptionKeys(
        @NotBlank @Size(max = 255) String p256dh,
        @NotBlank @Size(max = 255) String auth
    ) {
    }
}
