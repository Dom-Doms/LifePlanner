package it.univ.lifeplanner.user.dto;

import it.univ.lifeplanner.user.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @NotBlank @Size(max = 80) String username,
    @NotBlank @Email @Size(max = 160) String email,
    UserRole role
) {
}
