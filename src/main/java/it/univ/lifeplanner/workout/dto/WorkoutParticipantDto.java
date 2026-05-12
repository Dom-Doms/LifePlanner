package it.univ.lifeplanner.workout.dto;

import it.univ.lifeplanner.planning.model.ParticipantType;
import it.univ.lifeplanner.workout.model.WorkoutSessionParticipant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkoutParticipantDto(
    Long id,
    Long userId,
    @NotBlank @Size(max = 120) String displayName,
    ParticipantType participantType
) {
    public static WorkoutParticipantDto from(WorkoutSessionParticipant participant) {
        return new WorkoutParticipantDto(
            participant.getId(),
            participant.getRegisteredUser() == null ? null : participant.getRegisteredUser().getId(),
            participant.getDisplayName(),
            participant.getParticipantType()
        );
    }
}
