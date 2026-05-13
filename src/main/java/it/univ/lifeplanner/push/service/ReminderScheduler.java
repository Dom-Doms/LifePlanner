package it.univ.lifeplanner.push.service;

import it.univ.lifeplanner.planning.model.CalendarEvent;
import it.univ.lifeplanner.planning.model.EventParticipant;
import it.univ.lifeplanner.planning.repository.CalendarEventRepository;
import it.univ.lifeplanner.push.dto.PushPayload;
import it.univ.lifeplanner.user.model.AppUser;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {
    private static final ZoneId ZONE = ZoneId.of("Europe/Rome");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final CalendarEventRepository eventRepository;
    private final PushNotificationService pushNotificationService;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void sendDueReminders() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate tomorrow = today.plusDays(1);
        Instant now = Instant.now();
        eventRepository
            .findByReminderEnabledTrueAndReminderSentAtIsNullAndEventDateBetweenOrderByEventDateAscStartTimeAsc(today, tomorrow)
            .stream()
            .filter(event -> eventStartInstant(event).isAfter(now))
            .filter(event -> !notificationInstant(event).isAfter(now))
            .forEach(event -> {
                sendReminder(event);
                event.setReminderSentAt(now);
            });
    }

    private Instant notificationInstant(CalendarEvent event) {
        return eventStartInstant(event).minusSeconds((long) reminderMinutes(event) * 60);
    }

    private Instant eventStartInstant(CalendarEvent event) {
        LocalTime startTime = event.isAllDay() || event.getStartTime() == null ? LocalTime.of(9, 0) : event.getStartTime();
        return LocalDateTime.of(event.getEventDate(), startTime).atZone(ZONE).toInstant();
    }

    private int reminderMinutes(CalendarEvent event) {
        return event.getReminderMinutesBefore() == null ? 30 : event.getReminderMinutesBefore();
    }

    private void sendReminder(CalendarEvent event) {
        PushPayload payload = new PushPayload(
            event.getTitle(),
            body(event),
            "/day/" + event.getEventDate(),
            "event-" + event.getId()
        );
        recipients(event).values().forEach(user -> pushNotificationService.sendToUser(user, payload));
    }

    private Map<Long, AppUser> recipients(CalendarEvent event) {
        Map<Long, AppUser> users = new LinkedHashMap<>();
        users.put(event.getOwner().getId(), event.getOwner());
        for (EventParticipant participant : event.getParticipants()) {
            AppUser user = participant.getRegisteredUser();
            if (user != null) {
                users.put(user.getId(), user);
            }
        }
        return users;
    }

    private String body(CalendarEvent event) {
        if (event.isAllDay() || event.getStartTime() == null) {
            return event.getEventDate().equals(LocalDate.now(ZONE).plusDays(1)) ? "Domani" : "Oggi";
        }
        String when = event.getEventDate().equals(LocalDate.now(ZONE).plusDays(1)) ? "domani alle " : "alle ";
        return when + event.getStartTime().format(TIME_FORMAT);
    }
}
