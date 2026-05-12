package it.univ.lifeplanner.planning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DayContextRequest(
    @NotBlank @Size(max = 80) String label,
    @Size(max = 30) String color,
    @Size(max = 20) String emoji,
    Boolean active
) {
}
