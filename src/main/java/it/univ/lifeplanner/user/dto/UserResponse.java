package it.univ.lifeplanner.user.dto;

import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.user.model.UserRole;
import java.time.Instant;

public record UserResponse(
    Long id,
    String username,
    String displayName,
    String email,
    UserRole role,
    Instant createdAt,
    Instant updatedAt
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
