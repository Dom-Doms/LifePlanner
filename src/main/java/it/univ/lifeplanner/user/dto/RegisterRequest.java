package it.univ.lifeplanner.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 80) String username,
    @NotBlank @Email @Size(max = 160) String email,
    @NotBlank @Size(min = 6, max = 100) String password
) {
}
