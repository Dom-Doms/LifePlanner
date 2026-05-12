package it.univ.lifeplanner.user.service;

import it.univ.lifeplanner.common.BadRequestException;
import it.univ.lifeplanner.common.NotFoundException;
import it.univ.lifeplanner.user.dto.RegisterRequest;
import it.univ.lifeplanner.user.dto.UserResponse;
import it.univ.lifeplanner.user.dto.UserUpdateRequest;
import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.user.model.UserRole;
import it.univ.lifeplanner.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already registered");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already registered");
        }
        AppUser user = new AppUser();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserResponse.from(requireUser(id));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        AppUser user = requireUser(id);
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setRole(request.role() == null ? user.getRole() : request.role());
        return UserResponse.from(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.delete(requireUser(id));
    }

    private AppUser requireUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
