package it.univ.lifeplanner.planning.controller;

import it.univ.lifeplanner.planning.dto.DayContextRequest;
import it.univ.lifeplanner.planning.dto.DayContextResponse;
import it.univ.lifeplanner.planning.service.DayContextService;
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
@RequestMapping("/api/day-contexts")
@RequiredArgsConstructor
public class DayContextController {
    private final DayContextService service;

    @GetMapping
    public List<DayContextResponse> findAll() {
        return service.findAllForCurrentUser();
    }

    @PostMapping
    public DayContextResponse create(@Valid @RequestBody DayContextRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public DayContextResponse update(@PathVariable Long id, @Valid @RequestBody DayContextRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
