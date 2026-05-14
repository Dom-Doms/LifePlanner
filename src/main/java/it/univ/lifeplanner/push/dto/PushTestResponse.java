package it.univ.lifeplanner.push.dto;

import java.util.List;

public record PushTestResponse(
    int activeSubscriptions,
    int sent,
    int failed,
    List<String> errors
) {
}
