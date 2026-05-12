package it.univ.lifeplanner.planning.controller;

import it.univ.lifeplanner.planning.dto.CalendarEventRequest;
import it.univ.lifeplanner.planning.dto.CalendarEventResponse;
import it.univ.lifeplanner.planning.service.CalendarEventService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class CalendarEventController {
    private final CalendarEventService service;

    @GetMapping
    public List<CalendarEventResponse> findBetween(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return service.findBetween(from, to);
    }

    @GetMapping("/{id}")
    public CalendarEventResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public CalendarEventResponse create(@Valid @RequestBody CalendarEventRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CalendarEventResponse update(@PathVariable Long id, @Valid @RequestBody CalendarEventRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
