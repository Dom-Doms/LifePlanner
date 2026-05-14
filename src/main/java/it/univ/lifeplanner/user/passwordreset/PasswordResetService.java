package it.univ.lifeplanner.user.passwordreset;

import it.univ.lifeplanner.common.BadRequestException;
import it.univ.lifeplanner.user.dto.MessageResponse;
import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    private static final String GENERIC_MESSAGE = "Se l'email è registrata, riceverai le istruzioni.";
    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Transactional
    public MessageResponse requestReset(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresent(this::createTokenAndSendMail);
        return new MessageResponse(GENERIC_MESSAGE);
    }

    @Transactional
    public MessageResponse resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hashToken(token))
            .orElseThrow(() -> new BadRequestException("Token reset password non valido o scaduto."));
        Instant now = Instant.now();
        if (resetToken.getUsedAt() != null || resetToken.getExpiresAt().isBefore(now)) {
            throw new BadRequestException("Token reset password non valido o scaduto.");
        }
        AppUser user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        resetToken.setUsedAt(now);
        return new MessageResponse("Password aggiornata.");
    }

    private void createTokenAndSendMail(AppUser user) {
        String token = generateToken();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(hashToken(token));
        resetToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        tokenRepository.save(resetToken);
        try {
            mailSender.send(buildResetMessage(user, token));
        } catch (MailException ex) {
            log.warn("Password reset email send failed for userId={}: {}", user.getId(), ex.getMessage());
        }
    }

    private SimpleMailMessage buildResetMessage(AppUser user, String token) {
        String resetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
            .path("/reset-password")
            .queryParam("token", token)
            .build()
            .toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }
        message.setTo(user.getEmail());
        message.setSubject("Reset password LifePlanner");
        message.setText("""
            Hai richiesto il reset della password per LifePlanner.

            Apri questo link per scegliere una nuova password:
            %s

            Il link scade tra 30 minuti e può essere usato una sola volta.
            Se non hai richiesto tu il reset, ignora questa email.
            """.formatted(resetUrl));
        return message;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
