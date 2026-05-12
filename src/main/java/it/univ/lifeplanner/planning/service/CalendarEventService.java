package it.univ.lifeplanner.planning.service;

import it.univ.lifeplanner.common.ForbiddenException;
import it.univ.lifeplanner.common.NotFoundException;
import it.univ.lifeplanner.planning.dto.CalendarEventRequest;
import it.univ.lifeplanner.planning.dto.CalendarEventResponse;
import it.univ.lifeplanner.planning.dto.ParticipantDto;
import it.univ.lifeplanner.planning.model.CalendarEvent;
import it.univ.lifeplanner.planning.model.EventParticipant;
import it.univ.lifeplanner.planning.model.ParticipantType;
import it.univ.lifeplanner.planning.repository.CalendarEventRepository;
import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.user.repository.UserRepository;
import it.univ.lifeplanner.user.service.CurrentUserService;
import it.univ.lifeplanner.workout.service.WorkoutSessionService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalendarEventService {
    private final CalendarEventRepository repository;
    private final UserRepository userRepository;
    private final WorkoutSessionService workoutSessionService;
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
        return repository.findByOwnerAndEventDateBetweenOrderByEventDateAscAllDayDescStartTimeAsc(user, from, to)
            .stream().map(CalendarEventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CalendarEventResponse findById(Long id) {
        return CalendarEventResponse.from(requireOwned(id));
    }

    @Transactional
    public CalendarEventResponse create(CalendarEventRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        CalendarEvent event = new CalendarEvent();
        event.setOwner(user);
        apply(event, request);
        return CalendarEventResponse.from(repository.save(event));
    }

    @Transactional
    public CalendarEventResponse update(Long id, CalendarEventRequest request) {
        CalendarEvent event = requireOwned(id);
        apply(event, request);
        return CalendarEventResponse.from(event);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(requireOwned(id));
    }

    private CalendarEvent requireOwned(Long id) {
        AppUser user = currentUserService.requireCurrentUser();
        CalendarEvent event = repository.findById(id).orElseThrow(() -> new NotFoundException("Event not found"));
        if (!currentUserService.isAdmin(user) && !event.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot access another user's event");
        }
        return event;
    }

    private void apply(CalendarEvent event, CalendarEventRequest request) {
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setEventDate(request.eventDate());
        event.setStartTime(request.allDay() ? null : request.startTime());
        event.setEndTime(request.allDay() ? null : request.endTime());
        event.setAllDay(request.allDay());
        event.setType(request.type());
        event.setLocation(request.location());
        event.setColor(request.color());
        event.setWorkoutSession(request.workoutSessionId() == null ? null : workoutSessionService.requireOwnedEntity(request.workoutSessionId()));
        copyParticipants(event, request.participants());
    }

    private void copyParticipants(CalendarEvent event, List<ParticipantDto> participants) {
        event.getParticipants().clear();
        if (participants == null) {
            return;
        }
        for (ParticipantDto dto : participants) {
            EventParticipant participant = new EventParticipant();
            participant.setEvent(event);
            participant.setDisplayName(dto.displayName().trim());
            participant.setParticipantType(dto.participantType() == null ? ParticipantType.FREE_TEXT : dto.participantType());
            if (dto.userId() != null) {
                participant.setRegisteredUser(userRepository.findById(dto.userId()).orElseThrow(() -> new NotFoundException("Participant user not found")));
                participant.setParticipantType(ParticipantType.REGISTERED_USER);
            }
            event.getParticipants().add(participant);
        }
    }
}
