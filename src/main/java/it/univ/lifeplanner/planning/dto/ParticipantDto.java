package it.univ.lifeplanner.planning.dto;

import it.univ.lifeplanner.planning.model.EventParticipant;
import it.univ.lifeplanner.planning.model.ParticipantType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParticipantDto(
    Long id,
    Long userId,
    @NotBlank @Size(max = 120) String displayName,
    ParticipantType participantType
) {
    public static ParticipantDto from(EventParticipant participant) {
        return new ParticipantDto(
            participant.getId(),
            participant.getRegisteredUser() == null ? null : participant.getRegisteredUser().getId(),
            participant.getDisplayName(),
            participant.getParticipantType()
        );
    }
}
