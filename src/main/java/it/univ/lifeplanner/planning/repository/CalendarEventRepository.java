package it.univ.lifeplanner.planning.repository;

import it.univ.lifeplanner.planning.model.CalendarEvent;
import it.univ.lifeplanner.user.model.AppUser;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    List<CalendarEvent> findByOwnerAndEventDateBetweenOrderByEventDateAscAllDayDescStartTimeAsc(AppUser owner, LocalDate from, LocalDate to);

    @Query("""
        select distinct event from CalendarEvent event
        left join event.participants participant
        where event.eventDate between :from and :to
          and (event.owner = :user or participant.registeredUser = :user)
        order by event.eventDate asc, event.allDay desc, event.startTime asc
        """)
    List<CalendarEvent> findVisibleBetween(@Param("user") AppUser user, @Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByWorkoutSession_Template_Id(Long templateId);

    List<CalendarEvent> findByReminderEnabledTrueAndReminderSentAtIsNullAndEventDateBetweenOrderByEventDateAscStartTimeAsc(LocalDate from, LocalDate to);
}
