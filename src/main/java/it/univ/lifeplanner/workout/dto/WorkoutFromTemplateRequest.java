package it.univ.lifeplanner.workout.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record WorkoutFromTemplateRequest(
    @NotNull Long templateId,
    @NotNull LocalDate date,
    String title,
    String notes,
    @Valid List<WorkoutParticipantDto> participants
) {
}
