package it.univ.lifeplanner.planning.dto;

import it.univ.lifeplanner.planning.model.RecurrenceType;
import java.time.LocalDate;

public record DailyPlanRequest(
    Long contextId,
    String notes,
    RecurrenceType recurrenceType,
    LocalDate recurrenceUntil
) {
}
