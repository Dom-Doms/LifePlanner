package it.univ.lifeplanner.workout.service;

import it.univ.lifeplanner.common.ForbiddenException;
import it.univ.lifeplanner.common.NotFoundException;
import it.univ.lifeplanner.planning.repository.CalendarEventRepository;
import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.user.service.CurrentUserService;
import it.univ.lifeplanner.workout.dto.WorkoutExerciseDto;
import it.univ.lifeplanner.workout.dto.WorkoutTemplateRequest;
import it.univ.lifeplanner.workout.dto.WorkoutTemplateResponse;
import it.univ.lifeplanner.workout.model.WorkoutExercise;
import it.univ.lifeplanner.workout.model.WorkoutTemplate;
import it.univ.lifeplanner.workout.repository.WorkoutSessionRepository;
import it.univ.lifeplanner.workout.repository.WorkoutTemplateRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkoutTemplateService {
    private final WorkoutTemplateRepository repository;
    private final CurrentUserService currentUserService;
    private final WorkoutSessionRepository sessionRepository;
    private final CalendarEventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<WorkoutTemplateResponse> findAll() {
        AppUser user = currentUserService.requireCurrentUser();
        if (currentUserService.isAdmin(user)) {
            return repository.findByActiveTrueOrderByNameAsc().stream().map(WorkoutTemplateResponse::from).toList();
        }
        return repository.findByOwnerAndActiveTrueOrderByNameAsc(user).stream().map(WorkoutTemplateResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public WorkoutTemplateResponse findById(Long id) {
        return WorkoutTemplateResponse.from(requireOwned(id));
    }

    @Transactional
    public WorkoutTemplateResponse create(WorkoutTemplateRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        WorkoutTemplate template = new WorkoutTemplate();
        template.setOwner(user);
        apply(template, request);
        return WorkoutTemplateResponse.from(repository.save(template));
    }

    @Transactional
    public WorkoutTemplateResponse update(Long id, WorkoutTemplateRequest request) {
        WorkoutTemplate template = requireOwned(id);
        apply(template, request);
        return WorkoutTemplateResponse.from(template);
    }

    @Transactional
    public void delete(Long id) {
        WorkoutTemplate template = requireOwned(id);
        if (sessionRepository.existsByTemplate_Id(id) || eventRepository.countByWorkoutSession_Template_Id(id) > 0) {
            template.setActive(false);
            return;
        }
        repository.delete(template);
    }

    public WorkoutTemplate requireOwnedEntity(Long id) {
        return requireOwned(id);
    }

    private WorkoutTemplate requireOwned(Long id) {
        AppUser user = currentUserService.requireCurrentUser();
        WorkoutTemplate template = repository.findById(id).orElseThrow(() -> new NotFoundException("Workout template not found"));
        if (!currentUserService.isAdmin(user) && !template.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot access another user's workout template");
        }
        if (!template.isActive()) {
            throw new NotFoundException("Workout template not found");
        }
        return template;
    }

    private void apply(WorkoutTemplate template, WorkoutTemplateRequest request) {
        template.setName(request.name().trim());
        template.setDescription(request.description());
        template.setActive(true);
        template.getExercises().clear();
        if (request.exercises() == null) {
            return;
        }
        int index = 0;
        for (WorkoutExerciseDto dto : request.exercises()) {
            WorkoutExercise exercise = new WorkoutExercise();
            exercise.setTemplate(template);
            exercise.setName(dto.name().trim());
            exercise.setMuscleGroup(dto.muscleGroup());
            exercise.setSets(dto.sets());
            exercise.setReps(dto.reps());
            exercise.setSuggestedWeight(dto.suggestedWeight());
            exercise.setRestSeconds(dto.restSeconds());
            exercise.setNotes(dto.notes());
            exercise.setExerciseOrder(dto.exerciseOrder() > 0 ? dto.exerciseOrder() : index);
            template.getExercises().add(exercise);
            index++;
        }
    }
}
