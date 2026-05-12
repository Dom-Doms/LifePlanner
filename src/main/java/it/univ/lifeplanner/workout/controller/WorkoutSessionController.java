package it.univ.lifeplanner.workout.controller;

import it.univ.lifeplanner.workout.dto.WorkoutFromTemplateRequest;
import it.univ.lifeplanner.workout.dto.WorkoutSessionRequest;
import it.univ.lifeplanner.workout.dto.WorkoutSessionResponse;
import it.univ.lifeplanner.workout.service.WorkoutSessionService;
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
@RequestMapping("/api/workout-sessions")
@RequiredArgsConstructor
public class WorkoutSessionController {
    private final WorkoutSessionService service;

    @GetMapping
    public List<WorkoutSessionResponse> findBetween(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return service.findBetween(from, to);
    }

    @GetMapping("/{id}")
    public WorkoutSessionResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/date/{date}")
    public List<WorkoutSessionResponse> findByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.findByDate(date);
    }

    @PostMapping
    public WorkoutSessionResponse create(@Valid @RequestBody WorkoutSessionRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public WorkoutSessionResponse update(@PathVariable Long id, @Valid @RequestBody WorkoutSessionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/from-template")
    public WorkoutSessionResponse createFromTemplate(@Valid @RequestBody WorkoutFromTemplateRequest request) {
        return service.createFromTemplate(request);
    }
}
