package it.univ.lifeplanner.push.dto;

public record PushPayload(
    String title,
    String body,
    String url,
    String tag
) {
}
