package it.univ.lifeplanner.planning.service;

import it.univ.lifeplanner.common.BadRequestException;
import it.univ.lifeplanner.planning.dto.DailyPlanRequest;
import it.univ.lifeplanner.planning.dto.DailyPlanResponse;
import it.univ.lifeplanner.planning.model.DayContext;
import it.univ.lifeplanner.planning.model.DailyPlan;
import it.univ.lifeplanner.planning.model.RecurrenceType;
import it.univ.lifeplanner.planning.repository.DailyPlanRepository;
import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.user.service.CurrentUserService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DailyPlanService {
    private final DailyPlanRepository repository;
    private final DayContextService dayContextService;
    private final CurrentUserService currentUserService;

    @Transactional
    public DailyPlanResponse getOrCreate(LocalDate date) {
        AppUser user = currentUserService.requireCurrentUser();
        return DailyPlanResponse.from(repository.findByOwnerAndDate(user, date).orElseGet(() -> createEmpty(user, date)));
    }

    @Transactional
    public DailyPlanResponse update(LocalDate date, DailyPlanRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        RecurrenceType recurrenceType = request.recurrenceType() == null ? RecurrenceType.NONE : request.recurrenceType();
        if (recurrenceType != RecurrenceType.NONE && request.recurrenceUntil() == null) {
            throw new BadRequestException("Recurrence end date is required");
        }
        DailyPlan plan = repository.findByOwnerAndDate(user, date).orElseGet(() -> createEmpty(user, date));
        DayContext context = null;
        if (request.contextId() != null) {
            context = dayContextService.requireOwnedEntity(request.contextId());
            if (!context.getOwner().getId().equals(user.getId())) {
                throw new BadRequestException("Selected context does not belong to the current user");
            }
        }
        plan.setContext(context);
        plan.setNotes(request.notes());
        if (recurrenceType != RecurrenceType.NONE) {
            applyContextRecurrence(user, date, request, context, recurrenceType);
        }
        return DailyPlanResponse.from(plan);
    }

    @Transactional(readOnly = true)
    public List<DailyPlanResponse> week(LocalDate startDate) {
        AppUser user = currentUserService.requireCurrentUser();
        return repository.findByOwnerAndDateBetweenOrderByDateAsc(user, startDate, startDate.plusDays(6))
            .stream().map(DailyPlanResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DailyPlanResponse> month(int year, int month) {
        AppUser user = currentUserService.requireCurrentUser();
        YearMonth ym = YearMonth.of(year, month);
        return repository.findByOwnerAndDateBetweenOrderByDateAsc(user, ym.atDay(1), ym.atEndOfMonth())
            .stream().map(DailyPlanResponse::from).toList();
    }

    private DailyPlan createEmpty(AppUser user, LocalDate date) {
        DailyPlan plan = new DailyPlan();
        plan.setOwner(user);
        plan.setDate(date);
        return repository.save(plan);
    }

    private void applyContextRecurrence(AppUser user, LocalDate startDate, DailyPlanRequest request, DayContext context, RecurrenceType type) {
        LocalDate current = nextDate(startDate, type);
        while (!current.isAfter(request.recurrenceUntil())) {
            LocalDate targetDate = current;
            DailyPlan plan = repository.findByOwnerAndDate(user, targetDate).orElseGet(() -> createEmpty(user, targetDate));
            plan.setContext(context);
            plan.setNotes(request.notes());
            current = nextDate(current, type);
        }
    }

    private LocalDate nextDate(LocalDate date, RecurrenceType type) {
        return switch (type) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case BIWEEKLY -> date.plusWeeks(2);
            case MONTHLY -> date.plusMonths(1);
            case NONE -> date.plusDays(1);
        };
    }
}
