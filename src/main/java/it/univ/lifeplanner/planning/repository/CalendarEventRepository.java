package it.univ.lifeplanner.planning.repository;

import it.univ.lifeplanner.planning.model.CalendarEvent;
import it.univ.lifeplanner.user.model.AppUser;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    List<CalendarEvent> findByOwnerAndEventDateBetweenOrderByEventDateAscAllDayDescStartTimeAsc(AppUser owner, LocalDate from, LocalDate to);
}
