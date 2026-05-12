package it.univ.lifeplanner.workout.dto;

import it.univ.lifeplanner.workout.model.WorkoutExercise;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkoutExerciseDto(
    Long id,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 80) String muscleGroup,
    Integer sets,
    String reps,
    String suggestedWeight,
    Integer restSeconds,
    String notes,
    int exerciseOrder
) {
    public static WorkoutExerciseDto from(WorkoutExercise exercise) {
        return new WorkoutExerciseDto(
            exercise.getId(),
            exercise.getName(),
            exercise.getMuscleGroup(),
            exercise.getSets(),
            exercise.getReps(),
            exercise.getSuggestedWeight(),
            exercise.getRestSeconds(),
            exercise.getNotes(),
            exercise.getExerciseOrder()
        );
    }
}
