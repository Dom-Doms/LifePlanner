package it.univ.lifeplanner.user.service;

import it.univ.lifeplanner.common.UnauthorizedException;
import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.user.model.UserRole;
import it.univ.lifeplanner.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;

    public AppUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = authentication != null && authentication.getPrincipal() instanceof String principal
            && "anonymousUser".equals(principal);
        if (authentication == null || !authentication.isAuthenticated() || anonymous) {
            throw new UnauthorizedException("No authenticated user present");
        }
        return userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new UnauthorizedException("User not found: " + authentication.getName()));
    }

    public boolean isAdmin(AppUser user) {
        return user.getRole() == UserRole.ADMIN;
    }
}
