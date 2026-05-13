package it.univ.lifeplanner.planning.dto;

import it.univ.lifeplanner.planning.model.EventType;
import it.univ.lifeplanner.planning.model.RecurrenceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CalendarEventRequest(
    @NotBlank @Size(max = 160) String title,
    String description,
    @NotNull LocalDate eventDate,
    LocalTime startTime,
    LocalTime endTime,
    boolean allDay,
    @NotNull EventType type,
    @Size(max = 160) String location,
    @Size(max = 30) String color,
    Long workoutSessionId,
    Long workoutTemplateId,
    RecurrenceType recurrenceType,
    LocalDate recurrenceUntil,
    @Valid List<ParticipantDto> participants
) {
}
