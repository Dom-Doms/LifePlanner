package it.univ.lifeplanner.config;

import it.univ.lifeplanner.planning.model.CalendarEvent;
import it.univ.lifeplanner.planning.model.DayContext;
import it.univ.lifeplanner.planning.model.DailyPlan;
import it.univ.lifeplanner.planning.model.EventParticipant;
import it.univ.lifeplanner.planning.model.EventType;
import it.univ.lifeplanner.planning.model.ParticipantType;
import it.univ.lifeplanner.planning.repository.CalendarEventRepository;
import it.univ.lifeplanner.planning.repository.DayContextRepository;
import it.univ.lifeplanner.planning.repository.DailyPlanRepository;
import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.user.model.UserRole;
import it.univ.lifeplanner.user.repository.UserRepository;
import it.univ.lifeplanner.workout.model.WorkoutExercise;
import it.univ.lifeplanner.workout.model.WorkoutSession;
import it.univ.lifeplanner.workout.model.WorkoutSessionExercise;
import it.univ.lifeplanner.workout.model.WorkoutSessionParticipant;
import it.univ.lifeplanner.workout.model.WorkoutTemplate;
import it.univ.lifeplanner.workout.repository.WorkoutSessionRepository;
import it.univ.lifeplanner.workout.repository.WorkoutTemplateRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.dev-seed", name = "enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {
    private final UserRepository userRepository;
    private final DayContextRepository dayContextRepository;
    private final DailyPlanRepository dailyPlanRepository;
    private final CalendarEventRepository eventRepository;
    private final WorkoutTemplateRepository templateRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppUser admin = upsertUser("admin", "admin@example.com", UserRole.ADMIN);
        AppUser user = upsertUser("user", "user@example.com", UserRole.USER);
        seedForUser(admin);
        seedForUser(user);
    }

    private AppUser upsertUser(String username, String email, UserRole role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            AppUser user = new AppUser();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode("password"));
            user.setRole(role);
            return userRepository.save(user);
        });
    }

    private void seedForUser(AppUser owner) {
        if (!dayContextRepository.findByOwnerOrderByLabelAsc(owner).isEmpty()) {
            return;
        }
        DayContext domi = context(owner, "Da Domi", "#7c3aed", "home");
        context(owner, "Da Ritina", "#db2777", "heart");
        context(owner, "Da me", "#0284c7", "me");
        context(owner, "Da lei", "#16a34a", "leaf");

        LocalDate today = LocalDate.now();
        DailyPlan plan = new DailyPlan();
        plan.setOwner(owner);
        plan.setDate(today);
        plan.setContext(domi);
        plan.setNotes("Giornata di esempio pronta per test mobile.");
        dailyPlanRepository.save(plan);

        WorkoutTemplate push = template(owner, "Push Day", "Petto, spalle e tricipiti", List.of("Panca piana", "Military press", "Push down"));
        template(owner, "Pull Day", "Schiena e bicipiti", List.of("Lat machine", "Rematore", "Curl manubri"));
        template(owner, "Legs Day", "Gambe e core", List.of("Squat", "Leg press", "Calf raise"));

        WorkoutSession session = workoutSession(owner, today, push);
        event(owner, "Studio Analisi", today, LocalTime.of(9, 30), LocalTime.of(11, 0), EventType.STUDY, null);
        event(owner, "Esame", today.plusDays(2), null, null, EventType.EXAM, null);
        event(owner, "Palestra", today, LocalTime.of(18, 30), LocalTime.of(20, 0), EventType.GYM, session);
        event(owner, "Studio con Ritina", today.plusDays(1), LocalTime.of(15, 0), LocalTime.of(17, 0), EventType.STUDY, null);
    }

    private DayContext context(AppUser owner, String label, String color, String emoji) {
        DayContext context = new DayContext();
        context.setOwner(owner);
        context.setLabel(label);
        context.setColor(color);
        context.setEmoji(emoji);
        context.setActive(true);
        return dayContextRepository.save(context);
    }

    private WorkoutTemplate template(AppUser owner, String name, String description, List<String> exerciseNames) {
        WorkoutTemplate template = new WorkoutTemplate();
        template.setOwner(owner);
        template.setName(name);
        template.setDescription(description);
        int index = 0;
        for (String exerciseName : exerciseNames) {
            WorkoutExercise exercise = new WorkoutExercise();
            exercise.setTemplate(template);
            exercise.setName(exerciseName);
            exercise.setMuscleGroup("Full body");
            exercise.setSets(3);
            exercise.setReps("8-12");
            exercise.setSuggestedWeight("moderato");
            exercise.setRestSeconds(90);
            exercise.setExerciseOrder(index++);
            template.getExercises().add(exercise);
        }
        return templateRepository.save(template);
    }

    private WorkoutSession workoutSession(AppUser owner, LocalDate date, WorkoutTemplate template) {
        WorkoutSession session = new WorkoutSession();
        session.setOwner(owner);
        session.setDate(date);
        session.setTemplate(template);
        session.setTitle(template.getName());
        session.setNotes("Allenamento condivisibile.");
        WorkoutSessionParticipant participant = new WorkoutSessionParticipant();
        participant.setWorkoutSession(session);
        participant.setDisplayName("Domi");
        participant.setParticipantType(ParticipantType.FREE_TEXT);
        session.getParticipants().add(participant);
        int index = 0;
        for (WorkoutExercise source : template.getExercises()) {
            WorkoutSessionExercise exercise = new WorkoutSessionExercise();
            exercise.setSession(session);
            exercise.setName(source.getName());
            exercise.setMuscleGroup(source.getMuscleGroup());
            exercise.setPlannedSets(source.getSets());
            exercise.setPlannedReps(source.getReps());
            exercise.setPlannedWeight(source.getSuggestedWeight());
            exercise.setRestSeconds(source.getRestSeconds());
            exercise.setExerciseOrder(index++);
            session.getExercises().add(exercise);
        }
        return sessionRepository.save(session);
    }

    private void event(AppUser owner, String title, LocalDate date, LocalTime start, LocalTime end, EventType type, WorkoutSession workoutSession) {
        CalendarEvent event = new CalendarEvent();
        event.setOwner(owner);
        event.setTitle(title);
        event.setEventDate(date);
        event.setStartTime(start);
        event.setEndTime(end);
        event.setAllDay(start == null);
        event.setType(type);
        event.setWorkoutSession(workoutSession);
        EventParticipant participant = new EventParticipant();
        participant.setEvent(event);
        participant.setDisplayName(title.contains("Ritina") ? "Ritina" : owner.getUsername());
        participant.setParticipantType(title.contains("Ritina") ? ParticipantType.FREE_TEXT : ParticipantType.REGISTERED_USER);
        participant.setRegisteredUser(title.contains("Ritina") ? null : owner);
        event.getParticipants().add(participant);
        eventRepository.save(event);
    }
}
