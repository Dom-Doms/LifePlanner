package it.univ.lifeplanner.planning.service;

import it.univ.lifeplanner.common.ForbiddenException;
import it.univ.lifeplanner.common.BadRequestException;
import it.univ.lifeplanner.common.NotFoundException;
import it.univ.lifeplanner.planning.dto.CalendarEventRequest;
import it.univ.lifeplanner.planning.dto.CalendarEventResponse;
import it.univ.lifeplanner.planning.dto.ParticipantDto;
import it.univ.lifeplanner.planning.model.CalendarEvent;
import it.univ.lifeplanner.planning.model.EventParticipant;
import it.univ.lifeplanner.planning.model.ParticipantType;
import it.univ.lifeplanner.planning.model.RecurrenceType;
import it.univ.lifeplanner.planning.repository.CalendarEventRepository;
import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.user.repository.UserRepository;
import it.univ.lifeplanner.user.service.CurrentUserService;
import it.univ.lifeplanner.workout.dto.WorkoutParticipantDto;
import it.univ.lifeplanner.workout.model.WorkoutTemplate;
import it.univ.lifeplanner.workout.service.WorkoutSessionService;
import it.univ.lifeplanner.workout.service.WorkoutTemplateService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarEventService {
    private final CalendarEventRepository repository;
    private final UserRepository userRepository;
    private final WorkoutSessionService workoutSessionService;
    private final WorkoutTemplateService workoutTemplateService;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> findBetween(LocalDate from, LocalDate to) {
        AppUser user = currentUserService.requireCurrentUser();
        if (currentUserService.isAdmin(user)) {
            return repository.findAll().stream()
                .filter(event -> !event.getEventDate().isBefore(from) && !event.getEventDate().isAfter(to))
                .map(CalendarEventResponse::from)
                .toList();
        }
        return repository.findVisibleBetween(user, from, to)
            .stream().map(CalendarEventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CalendarEventResponse findById(Long id) {
        return CalendarEventResponse.from(requireReadable(id));
    }

    @Transactional
    public CalendarEventResponse create(CalendarEventRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        log.info("Creating calendar event for userId={} request={}", user.getId(), request);
        validateEvent(request);
        validateRecurrence(request);
        try {
            CalendarEvent first = null;
            LocalDate current = request.eventDate();
            LocalDate last = recurrenceLastDate(request);
            while (!current.isAfter(last)) {
                CalendarEvent event = new CalendarEvent();
                event.setOwner(user);
                apply(event, request, current);
                CalendarEvent saved = repository.saveAndFlush(event);
                if (first == null) {
                    first = saved;
                }
                current = nextDate(current, recurrenceType(request));
            }
            return CalendarEventResponse.from(first);
        } catch (RuntimeException ex) {
            log.error("Calendar event creation failed for userId={} request={}", user.getId(), request, ex);
            throw ex;
        }
    }

    @Transactional
    public CalendarEventResponse update(Long id, CalendarEventRequest request) {
        CalendarEvent event = requireOwned(id);
        log.info("Updating calendar event id={} request={}", id, request);
        validateEvent(request);
        validateRecurrence(request);
        try {
            apply(event, request, request.eventDate());
            repository.flush();
            return CalendarEventResponse.from(event);
        } catch (RuntimeException ex) {
            log.error("Calendar event update failed for id={} request={}", id, request, ex);
            throw ex;
        }
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(requireOwned(id));
    }

    private CalendarEvent requireOwned(Long id) {
        AppUser user = currentUserService.requireCurrentUser();
        CalendarEvent event = repository.findById(id).orElseThrow(() -> new NotFoundException("Event not found"));
        if (!currentUserService.isAdmin(user) && !event.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot modify another user's event");
        }
        return event;
    }

    private CalendarEvent requireReadable(Long id) {
        AppUser user = currentUserService.requireCurrentUser();
        CalendarEvent event = repository.findById(id).orElseThrow(() -> new NotFoundException("Event not found"));
        boolean participant = event.getParticipants().stream()
            .anyMatch(item -> item.getRegisteredUser() != null && item.getRegisteredUser().getId().equals(user.getId()));
        if (!currentUserService.isAdmin(user) && !event.getOwner().getId().equals(user.getId()) && !participant) {
            throw new ForbiddenException("Cannot access another user's event");
        }
        return event;
    }

    private void apply(CalendarEvent event, CalendarEventRequest request, LocalDate eventDate) {
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setEventDate(eventDate);
        event.setStartTime(request.allDay() ? null : request.startTime());
        event.setEndTime(request.allDay() ? null : request.endTime());
        event.setAllDay(request.allDay());
        event.setType(request.type() == null ? it.univ.lifeplanner.planning.model.EventType.OTHER : request.type());
        event.setLocation(request.location());
        event.setColor(request.color());
        event.setRecurrenceType(recurrenceType(request));
        event.setRecurrenceUntil(request.recurrenceUntil());
        applyReminder(event, request);
        if (request.workoutTemplateId() != null) {
            WorkoutTemplate template = workoutTemplateService.requireOwnedEntity(request.workoutTemplateId());
            event.setWorkoutSession(workoutSessionService.createFromTemplateEntity(
                event.getOwner(),
                template,
                eventDate,
                request.title(),
                request.description(),
                toWorkoutParticipants(request.participants())
            ));
        } else {
            event.setWorkoutSession(request.workoutSessionId() == null ? null : workoutSessionService.requireOwnedEntity(request.workoutSessionId()));
        }
        copyParticipants(event, request.participants());
    }

    private void copyParticipants(CalendarEvent event, List<ParticipantDto> participants) {
        event.getParticipants().clear();
        if (participants == null) {
            return;
        }
        for (ParticipantDto dto : participants) {
            if (dto == null) {
                throw new BadRequestException("Participant cannot be empty");
            }
            validateParticipant(dto);
            EventParticipant participant = new EventParticipant();
            participant.setEvent(event);
            participant.setParticipantType(dto.participantType() == null ? ParticipantType.FREE_TEXT : dto.participantType());
            if (dto.registeredUserId() != null) {
                AppUser registeredUser = userRepository.findById(dto.registeredUserId()).orElseThrow(() -> new BadRequestException("Participant user not found"));
                participant.setRegisteredUser(registeredUser);
                participant.setDisplayName(registeredUser.getUsername());
                participant.setParticipantType(ParticipantType.REGISTERED_USER);
            } else {
                participant.setDisplayName(dto.displayName().trim());
            }
            event.getParticipants().add(participant);
        }
    }

    private List<WorkoutParticipantDto> toWorkoutParticipants(List<ParticipantDto> participants) {
        if (participants == null) {
            return List.of();
        }
        return participants.stream()
            .filter(item -> item != null)
            .map(item -> new WorkoutParticipantDto(item.id(), item.registeredUserId(), item.displayName(), item.participantType()))
            .toList();
    }

    private void validateEvent(CalendarEventRequest request) {
        if (!request.allDay() && (request.startTime() == null || request.endTime() == null)) {
            throw new BadRequestException("Start and end time are required for timed events");
        }
        if (!request.allDay() && !request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("End time must be after start time");
        }
        validateReminder(request);
    }

    private void validateParticipant(ParticipantDto participant) {
        ParticipantType type = participant.participantType() == null ? ParticipantType.FREE_TEXT : participant.participantType();
        if (type == ParticipantType.REGISTERED_USER && participant.registeredUserId() == null) {
            throw new BadRequestException("Registered participant user is required");
        }
        if (type == ParticipantType.FREE_TEXT && (participant.displayName() == null || participant.displayName().isBlank())) {
            throw new BadRequestException("Free text participant name is required");
        }
    }

    private void applyReminder(CalendarEvent event, CalendarEventRequest request) {
        boolean reminderEnabled = Boolean.TRUE.equals(request.reminderEnabled());
        event.setReminderEnabled(reminderEnabled);
        if (!reminderEnabled) {
            event.setReminderMinutesBefore(null);
            event.setReminderSentAt(null);
            return;
        }
        validateReminder(request);
        event.setReminderMinutesBefore(request.reminderMinutesBefore());
        event.setReminderSentAt(null);
    }

    private void validateReminder(CalendarEventRequest request) {
        if (!Boolean.TRUE.equals(request.reminderEnabled())) {
            return;
        }
        if (request.reminderMinutesBefore() == null || request.reminderMinutesBefore() <= 0) {
            throw new BadRequestException("Reminder minutes before must be greater than 0");
        }
        if (!List.of(10, 30, 60, 1440).contains(request.reminderMinutesBefore())) {
            throw new BadRequestException("Unsupported reminder value");
        }
    }

    private void validateRecurrence(CalendarEventRequest request) {
        RecurrenceType type = recurrenceType(request);
        if (type != RecurrenceType.NONE && request.recurrenceUntil() == null) {
            throw new BadRequestException("Recurrence end date is required");
        }
        if (type != RecurrenceType.NONE && request.recurrenceUntil().isBefore(request.eventDate())) {
            throw new BadRequestException("Recurrence end date cannot be before event date");
        }
    }

    private RecurrenceType recurrenceType(CalendarEventRequest request) {
        return request.recurrenceType() == null ? RecurrenceType.NONE : request.recurrenceType();
    }

    private LocalDate recurrenceLastDate(CalendarEventRequest request) {
        return recurrenceType(request) == RecurrenceType.NONE ? request.eventDate() : request.recurrenceUntil();
    }

    private LocalDate nextDate(LocalDate date, RecurrenceType type) {
        return switch (type) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case BIWEEKLY -> date.plusWeeks(2);
            case MONTHLY -> date.plusMonths(1);
            case NONE -> date.plusDays(1);
        };
    }
}
