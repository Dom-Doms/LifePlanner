package it.univ.lifeplanner.workout.repository;

import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.workout.model.WorkoutSession;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {
    List<WorkoutSession> findByOwnerAndDateBetweenOrderByDateAsc(AppUser owner, LocalDate from, LocalDate to);
    List<WorkoutSession> findByOwnerAndDateOrderByIdAsc(AppUser owner, LocalDate date);
}
