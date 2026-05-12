package it.univ.lifeplanner.workout.dto;

import it.univ.lifeplanner.workout.model.WorkoutTemplate;
import java.time.Instant;
import java.util.List;

public record WorkoutTemplateResponse(
    Long id,
    String name,
    String description,
    List<WorkoutExerciseDto> exercises,
    Instant createdAt,
    Instant updatedAt
) {
    public static WorkoutTemplateResponse from(WorkoutTemplate template) {
        return new WorkoutTemplateResponse(
            template.getId(),
            template.getName(),
            template.getDescription(),
            template.getExercises().stream().map(WorkoutExerciseDto::from).toList(),
            template.getCreatedAt(),
            template.getUpdatedAt()
        );
    }
}
