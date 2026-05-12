package it.univ.lifeplanner.workout.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record WorkoutSessionRequest(
    @NotNull LocalDate date,
    Long templateId,
    @NotBlank @Size(max = 120) String title,
    String notes,
    @Valid List<WorkoutParticipantDto> participants,
    @Valid List<WorkoutSessionExerciseDto> exercises
) {
}
