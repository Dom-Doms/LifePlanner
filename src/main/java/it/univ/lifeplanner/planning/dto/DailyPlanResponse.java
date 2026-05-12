package it.univ.lifeplanner.planning.dto;

import it.univ.lifeplanner.planning.model.DailyPlan;
import java.time.Instant;
import java.time.LocalDate;

public record DailyPlanResponse(
    Long id,
    LocalDate date,
    DayContextResponse context,
    String notes,
    Instant createdAt,
    Instant updatedAt
) {
    public static DailyPlanResponse from(DailyPlan plan) {
        return new DailyPlanResponse(
            plan.getId(),
            plan.getDate(),
            DayContextResponse.from(plan.getContext()),
            plan.getNotes(),
            plan.getCreatedAt(),
            plan.getUpdatedAt()
        );
    }
}
