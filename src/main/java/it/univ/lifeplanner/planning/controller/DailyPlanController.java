package it.univ.lifeplanner.planning.controller;

import it.univ.lifeplanner.planning.dto.DailyPlanRequest;
import it.univ.lifeplanner.planning.dto.DailyPlanResponse;
import it.univ.lifeplanner.planning.service.DailyPlanService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/daily-plans")
@RequiredArgsConstructor
public class DailyPlanController {
    private final DailyPlanService service;

    @GetMapping("/date/{date}")
    public DailyPlanResponse getByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getOrCreate(date);
    }

    @PutMapping("/date/{date}")
    public DailyPlanResponse updateByDate(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestBody DailyPlanRequest request
    ) {
        return service.update(date, request);
    }

    @GetMapping("/week")
    public List<DailyPlanResponse> week(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        return service.week(startDate);
    }

    @GetMapping("/month")
    public List<DailyPlanResponse> month(@RequestParam int year, @RequestParam int month) {
        return service.month(year, month);
    }
}
