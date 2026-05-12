package it.univ.lifeplanner.workout.repository;

import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.workout.model.WorkoutTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, Long> {
    List<WorkoutTemplate> findByOwnerOrderByNameAsc(AppUser owner);
}
