package it.univ.lifeplanner.planning.dto;

public record DailyPlanRequest(
    Long contextId,
    String notes
) {
}
