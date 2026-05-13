package it.univ.lifeplanner.push.dto;

import jakarta.validation.constraints.Size;

public record PushTestRequest(
    @Size(max = 120) String title,
    @Size(max = 240) String body,
    @Size(max = 255) String url
) {
}
