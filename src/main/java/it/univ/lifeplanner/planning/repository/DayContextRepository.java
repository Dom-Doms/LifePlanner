package it.univ.lifeplanner.planning.repository;

import it.univ.lifeplanner.planning.model.DayContext;
import it.univ.lifeplanner.user.model.AppUser;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DayContextRepository extends JpaRepository<DayContext, Long> {
    List<DayContext> findByOwnerOrderByLabelAsc(AppUser owner);
}
