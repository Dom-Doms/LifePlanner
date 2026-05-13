package it.univ.lifeplanner.workout.service;

import it.univ.lifeplanner.common.ForbiddenException;
import it.univ.lifeplanner.common.NotFoundException;
import it.univ.lifeplanner.planning.model.ParticipantType;
import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.user.repository.UserRepository;
import it.univ.lifeplanner.user.service.CurrentUserService;
import it.univ.lifeplanner.workout.dto.WorkoutFromTemplateRequest;
import it.univ.lifeplanner.workout.dto.WorkoutParticipantDto;
import it.univ.lifeplanner.workout.dto.WorkoutSessionExerciseDto;
import it.univ.lifeplanner.workout.dto.WorkoutSessionRequest;
import it.univ.lifeplanner.workout.dto.WorkoutSessionResponse;
import it.univ.lifeplanner.workout.model.WorkoutExercise;
import it.univ.lifeplanner.workout.model.WorkoutSession;
import it.univ.lifeplanner.workout.model.WorkoutSessionExercise;
import it.univ.lifeplanner.workout.model.WorkoutSessionParticipant;
import it.univ.lifeplanner.workout.model.WorkoutTemplate;
import it.univ.lifeplanner.workout.repository.WorkoutSessionRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkoutSessionService {
    private final WorkoutSessionRepository repository;
    private final WorkoutTemplateService templateService;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<WorkoutSessionResponse> findBetween(LocalDate from, LocalDate to) {
        AppUser user = currentUserService.requireCurrentUser();
        if (currentUserService.isAdmin(user)) {
            return repository.findAll().stream()
                .filter(session -> !session.getDate().isBefore(from) && !session.getDate().isAfter(to))
                .map(WorkoutSessionResponse::from)
                .toList();
        }
        return repository.findVisibleBetween(user, from, to).stream().map(WorkoutSessionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<WorkoutSessionResponse> findByDate(LocalDate date) {
        AppUser user = currentUserService.requireCurrentUser();
        if (currentUserService.isAdmin(user)) {
            return repository.findAll().stream()
                .filter(session -> session.getDate().equals(date))
                .map(WorkoutSessionResponse::from)
                .toList();
        }
        return repository.findVisibleByDate(user, date).stream().map(WorkoutSessionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public WorkoutSessionResponse findById(Long id) {
        return WorkoutSessionResponse.from(requireOwned(id));
    }

    @Transactional
    public WorkoutSessionResponse create(WorkoutSessionRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        WorkoutSession session = new WorkoutSession();
        session.setOwner(user);
        apply(session, request);
        return WorkoutSessionResponse.from(repository.save(session));
    }

    @Transactional
    public WorkoutSessionResponse update(Long id, WorkoutSessionRequest request) {
        WorkoutSession session = requireOwned(id);
        apply(session, request);
        return WorkoutSessionResponse.from(session);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(requireOwned(id));
    }

    @Transactional
    public WorkoutSessionResponse createFromTemplate(WorkoutFromTemplateRequest request) {
        WorkoutTemplate template = templateService.requireOwnedEntity(request.templateId());
        return WorkoutSessionResponse.from(repository.save(createFromTemplateEntity(
            currentUserService.requireCurrentUser(),
            template,
            request.date(),
            request.title(),
            request.notes(),
            request.participants()
        )));
    }

    public WorkoutSession createFromTemplateEntity(
        AppUser owner,
        WorkoutTemplate template,
        LocalDate date,
        String title,
        String notes,
        List<WorkoutParticipantDto> participants
    ) {
        WorkoutSession session = new WorkoutSession();
        session.setOwner(owner);
        session.setTemplate(template);
        session.setDate(date);
        session.setTitle(title == null || title.isBlank() ? template.getName() : title.trim());
        session.setNotes(notes);
        copyParticipants(session, participants);
        int index = 0;
        for (WorkoutExercise exercise : template.getExercises()) {
            WorkoutSessionExercise copy = new WorkoutSessionExercise();
            copy.setSession(session);
            copy.setName(exercise.getName());
            copy.setMuscleGroup(exercise.getMuscleGroup());
            copy.setPlannedSets(exercise.getSets());
            copy.setPlannedReps(exercise.getReps());
            copy.setPlannedWeight(exercise.getSuggestedWeight());
            copy.setRestSeconds(exercise.getRestSeconds());
            copy.setNotes(exercise.getNotes());
            copy.setExerciseOrder(index++);
            session.getExercises().add(copy);
        }
        return session;
    }

    public WorkoutSession requireOwnedEntity(Long id) {
        return requireOwned(id);
    }

    private WorkoutSession requireOwned(Long id) {
        AppUser user = currentUserService.requireCurrentUser();
        WorkoutSession session = repository.findById(id).orElseThrow(() -> new NotFoundException("Workout session not found"));
        if (!currentUserService.isAdmin(user) && !session.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot access another user's workout session");
        }
        return session;
    }

    private void apply(WorkoutSession session, WorkoutSessionRequest request) {
        session.setDate(request.date());
        session.setTitle(request.title().trim());
        session.setNotes(request.notes());
        session.setTemplate(request.templateId() == null ? null : templateService.requireOwnedEntity(request.templateId()));
        copyParticipants(session, request.participants());
        session.getExercises().clear();
        if (request.exercises() != null) {
            for (WorkoutSessionExerciseDto dto : request.exercises()) {
                WorkoutSessionExercise exercise = new WorkoutSessionExercise();
                exercise.setSession(session);
                exercise.setName(dto.name().trim());
                exercise.setMuscleGroup(dto.muscleGroup());
                exercise.setPlannedSets(dto.plannedSets());
                exercise.setPlannedReps(dto.plannedReps());
                exercise.setPlannedWeight(dto.plannedWeight());
                exercise.setActualSets(dto.actualSets());
                exercise.setActualReps(dto.actualReps());
                exercise.setActualWeight(dto.actualWeight());
                exercise.setRestSeconds(dto.restSeconds());
                exercise.setNotes(dto.notes());
                exercise.setExerciseOrder(dto.exerciseOrder());
                session.getExercises().add(exercise);
            }
        }
    }

    private void copyParticipants(WorkoutSession session, List<WorkoutParticipantDto> participants) {
        session.getParticipants().clear();
        if (participants == null) {
            return;
        }
        for (WorkoutParticipantDto dto : participants) {
            WorkoutSessionParticipant participant = new WorkoutSessionParticipant();
            participant.setWorkoutSession(session);
            participant.setParticipantType(dto.participantType() == null ? ParticipantType.FREE_TEXT : dto.participantType());
            if (dto.userId() != null) {
                AppUser registeredUser = userRepository.findById(dto.userId()).orElseThrow(() -> new NotFoundException("Participant user not found"));
                participant.setRegisteredUser(registeredUser);
                participant.setDisplayName(registeredUser.getUsername());
                participant.setParticipantType(ParticipantType.REGISTERED_USER);
            } else {
                participant.setDisplayName(dto.displayName().trim());
            }
            session.getParticipants().add(participant);
        }
    }
}
