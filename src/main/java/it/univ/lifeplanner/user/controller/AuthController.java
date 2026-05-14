package it.univ.lifeplanner.user.controller;

import it.univ.lifeplanner.security.JwtTokenService;
import it.univ.lifeplanner.security.UserPrincipal;
import it.univ.lifeplanner.user.dto.AuthResponse;
import it.univ.lifeplanner.user.dto.ForgotPasswordRequest;
import it.univ.lifeplanner.user.dto.LoginRequest;
import it.univ.lifeplanner.user.dto.MessageResponse;
import it.univ.lifeplanner.user.dto.RegisterRequest;
import it.univ.lifeplanner.user.dto.ResetPasswordRequest;
import it.univ.lifeplanner.user.dto.UserResponse;
import it.univ.lifeplanner.user.passwordreset.PasswordResetService;
import it.univ.lifeplanner.user.repository.UserRepository;
import it.univ.lifeplanner.user.service.CurrentUserService;
import it.univ.lifeplanner.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtTokenService.generateToken(principal);
        return new AuthResponse(token, userRepository.findByEmail(principal.getUsername()).map(UserResponse::from).orElseThrow());
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return passwordResetService.requestReset(request.email());
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return passwordResetService.resetPassword(request.token(), request.newPassword());
    }

    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(currentUserService.requireCurrentUser());
    }
}
