package it.univ.lifeplanner.workout.dto;

import it.univ.lifeplanner.workout.model.WorkoutSessionExercise;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkoutSessionExerciseDto(
    Long id,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 80) String muscleGroup,
    Integer plannedSets,
    String plannedReps,
    String plannedWeight,
    Integer actualSets,
    String actualReps,
    String actualWeight,
    Integer restSeconds,
    String notes,
    int exerciseOrder
) {
    public static WorkoutSessionExerciseDto from(WorkoutSessionExercise exercise) {
        return new WorkoutSessionExerciseDto(
            exercise.getId(),
            exercise.getName(),
            exercise.getMuscleGroup(),
            exercise.getPlannedSets(),
            exercise.getPlannedReps(),
            exercise.getPlannedWeight(),
            exercise.getActualSets(),
            exercise.getActualReps(),
            exercise.getActualWeight(),
            exercise.getRestSeconds(),
            exercise.getNotes(),
            exercise.getExerciseOrder()
        );
    }
}
