package it.univ.lifeplanner.workout.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record WorkoutTemplateRequest(
    @NotBlank @Size(max = 120) String name,
    String description,
    @Valid List<WorkoutExerciseDto> exercises
) {
}
