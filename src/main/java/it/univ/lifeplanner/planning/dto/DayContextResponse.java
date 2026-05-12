package it.univ.lifeplanner.planning.dto;

import it.univ.lifeplanner.planning.model.DayContext;
import java.time.Instant;

public record DayContextResponse(
    Long id,
    String label,
    String color,
    String emoji,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static DayContextResponse from(DayContext context) {
        if (context == null) {
            return null;
        }
        return new DayContextResponse(
            context.getId(),
            context.getLabel(),
            context.getColor(),
            context.getEmoji(),
            context.isActive(),
            context.getCreatedAt(),
            context.getUpdatedAt()
        );
    }
}
