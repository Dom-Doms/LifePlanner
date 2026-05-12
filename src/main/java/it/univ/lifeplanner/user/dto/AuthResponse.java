package it.univ.lifeplanner.user.dto;

public record AuthResponse(
    String token,
    UserResponse user
) {
}
