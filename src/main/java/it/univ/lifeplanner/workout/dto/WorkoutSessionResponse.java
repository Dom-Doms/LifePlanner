package it.univ.lifeplanner.workout.dto;

import it.univ.lifeplanner.workout.model.WorkoutSession;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WorkoutSessionResponse(
    Long id,
    LocalDate date,
    Long templateId,
    String title,
    String notes,
    List<WorkoutParticipantDto> participants,
    List<WorkoutSessionExerciseDto> exercises,
    Instant createdAt,
    Instant updatedAt
) {
    public static WorkoutSessionResponse from(WorkoutSession session) {
        return new WorkoutSessionResponse(
            session.getId(),
            session.getDate(),
            session.getTemplate() == null ? null : session.getTemplate().getId(),
            session.getTitle(),
            session.getNotes(),
            session.getParticipants().stream().map(WorkoutParticipantDto::from).toList(),
            session.getExercises().stream().map(WorkoutSessionExerciseDto::from).toList(),
            session.getCreatedAt(),
            session.getUpdatedAt()
        );
    }
}
