package it.univ.lifeplanner.planning.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import it.univ.lifeplanner.planning.model.EventParticipant;
import it.univ.lifeplanner.planning.model.ParticipantType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParticipantDto(
    Long id,
    @JsonAlias("userId") Long registeredUserId,
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
