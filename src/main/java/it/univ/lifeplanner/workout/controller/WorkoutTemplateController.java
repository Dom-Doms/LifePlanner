package it.univ.lifeplanner.workout.controller;

import it.univ.lifeplanner.workout.dto.WorkoutTemplateRequest;
import it.univ.lifeplanner.workout.dto.WorkoutTemplateResponse;
import it.univ.lifeplanner.workout.service.WorkoutTemplateService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workout-templates")
@RequiredArgsConstructor
public class WorkoutTemplateController {
    private final WorkoutTemplateService service;

    @GetMapping
    public List<WorkoutTemplateResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public WorkoutTemplateResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public WorkoutTemplateResponse create(@Valid @RequestBody WorkoutTemplateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public WorkoutTemplateResponse update(@PathVariable Long id, @Valid @RequestBody WorkoutTemplateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
