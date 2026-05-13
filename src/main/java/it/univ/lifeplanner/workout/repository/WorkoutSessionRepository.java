package it.univ.lifeplanner.workout.repository;

import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.workout.model.WorkoutSession;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {
    List<WorkoutSession> findByOwnerAndDateBetweenOrderByDateAsc(AppUser owner, LocalDate from, LocalDate to);
    List<WorkoutSession> findByOwnerAndDateOrderByIdAsc(AppUser owner, LocalDate date);
    boolean existsByTemplate_Id(Long templateId);

    @Query("""
        select distinct session from WorkoutSession session
        left join session.participants participant
        where session.date between :from and :to
          and (session.owner = :user or participant.registeredUser = :user)
        order by session.date asc
        """)
    List<WorkoutSession> findVisibleBetween(@Param("user") AppUser user, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
        select distinct session from WorkoutSession session
        left join session.participants participant
        where session.date = :date
          and (session.owner = :user or participant.registeredUser = :user)
        order by session.id asc
        """)
    List<WorkoutSession> findVisibleByDate(@Param("user") AppUser user, @Param("date") LocalDate date);
}
