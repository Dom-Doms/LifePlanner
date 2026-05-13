package it.univ.lifeplanner.planning.dto;

import it.univ.lifeplanner.planning.model.CalendarEvent;
import it.univ.lifeplanner.planning.model.EventType;
import it.univ.lifeplanner.planning.model.RecurrenceType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CalendarEventResponse(
    Long id,
    String title,
    String description,
    LocalDate eventDate,
    LocalTime startTime,
    LocalTime endTime,
    boolean allDay,
    EventType type,
    String location,
    String color,
    Long workoutSessionId,
    Long workoutTemplateId,
    RecurrenceType recurrenceType,
    LocalDate recurrenceUntil,
    Boolean reminderEnabled,
    Integer reminderMinutesBefore,
    Instant reminderSentAt,
    List<ParticipantDto> participants,
    Instant createdAt,
    Instant updatedAt
) {
    public static CalendarEventResponse from(CalendarEvent event) {
        return new CalendarEventResponse(
            event.getId(),
            event.getTitle(),
            event.getDescription(),
            event.getEventDate(),
            event.getStartTime(),
            event.getEndTime(),
            event.isAllDay(),
            event.getType(),
            event.getLocation(),
            event.getColor(),
            event.getWorkoutSession() == null ? null : event.getWorkoutSession().getId(),
            event.getWorkoutSession() == null || event.getWorkoutSession().getTemplate() == null ? null : event.getWorkoutSession().getTemplate().getId(),
            event.getRecurrenceType(),
            event.getRecurrenceUntil(),
            event.getReminderEnabled(),
            event.getReminderMinutesBefore(),
            event.getReminderSentAt(),
            event.getParticipants().stream().map(ParticipantDto::from).toList(),
            event.getCreatedAt(),
            event.getUpdatedAt()
        );
    }
}
