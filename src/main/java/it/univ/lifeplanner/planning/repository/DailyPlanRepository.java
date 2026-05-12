package it.univ.lifeplanner.planning.repository;

import it.univ.lifeplanner.planning.model.DailyPlan;
import it.univ.lifeplanner.user.model.AppUser;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {
    Optional<DailyPlan> findByOwnerAndDate(AppUser owner, LocalDate date);
    List<DailyPlan> findByOwnerAndDateBetweenOrderByDateAsc(AppUser owner, LocalDate from, LocalDate to);
}
