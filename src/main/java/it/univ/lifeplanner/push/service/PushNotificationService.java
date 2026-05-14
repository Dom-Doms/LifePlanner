package it.univ.lifeplanner.push.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.univ.lifeplanner.common.BadRequestException;
import it.univ.lifeplanner.push.dto.PushPayload;
import it.univ.lifeplanner.push.dto.PushSubscriptionRequest;
import it.univ.lifeplanner.push.dto.PushSubscriptionResponse;
import it.univ.lifeplanner.push.dto.PushTestResponse;
import it.univ.lifeplanner.push.model.PushSubscriptionEntity;
import it.univ.lifeplanner.push.repository.PushSubscriptionRepository;
import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.user.service.CurrentUserService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {
    private final PushSubscriptionRepository repository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    @Value("${app.push.vapid.public-key:}")
    private String vapidPublicKey;

    @Value("${app.push.vapid.private-key:}")
    private String vapidPrivateKey;

    @Value("${app.push.vapid.subject:mailto:admin@lifeplanner.local}")
    private String vapidSubject;

    @Transactional(readOnly = true)
    public String getPublicKey() {
        if (!hasVapidKeys()) {
            throw new BadRequestException("Le notifiche push non sono configurate sul server. Configura APP_PUSH_VAPID_PUBLIC_KEY e APP_PUSH_VAPID_PRIVATE_KEY.");
        }
        return normalizedVapidPublicKey();
    }

    @Transactional
    public PushSubscriptionResponse saveForCurrentUser(PushSubscriptionRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        PushSubscriptionEntity subscription = repository.findByEndpoint(request.endpoint())
            .orElseGet(PushSubscriptionEntity::new);
        subscription.setUser(user);
        subscription.setEndpoint(request.endpoint());
        subscription.setP256dh(request.keys().p256dh());
        subscription.setAuth(request.keys().auth());
        subscription.setUserAgent(request.userAgent());
        subscription.setActive(true);
        return PushSubscriptionResponse.from(repository.save(subscription));
    }

    @Transactional(readOnly = true)
    public List<PushSubscriptionResponse> findCurrentUserSubscriptions() {
        AppUser user = currentUserService.requireCurrentUser();
        return repository.findByUserAndActiveTrue(user).stream().map(PushSubscriptionResponse::from).toList();
    }

    @Transactional
    public void deactivateForCurrentUser(String endpoint) {
        AppUser user = currentUserService.requireCurrentUser();
        repository.findByEndpoint(endpoint)
            .filter(subscription -> subscription.getUser().getId().equals(user.getId()))
            .ifPresent(subscription -> subscription.setActive(false));
    }

    @Transactional
    public PushTestResponse sendTestToCurrentUser(String title, String body, String url) {
        if (!hasVapidKeys()) {
            throw new BadRequestException("Le notifiche push non sono configurate sul server. Verifica app.push.vapid.public-key, app.push.vapid.private-key e app.push.vapid.subject.");
        }
        AppUser user = currentUserService.requireCurrentUser();
        List<PushSubscriptionEntity> subscriptions = repository.findByUserIdAndActiveTrue(user.getId());
        if (subscriptions.isEmpty()) {
            return new PushTestResponse(
                0,
                0,
                0,
                List.of("Nessuna subscription push attiva per questo utente.")
            );
        }

        PushPayload payload = new PushPayload(
            blankToDefault(title, "Test LifePlanner"),
            blankToDefault(body, "Le notifiche funzionano"),
            blankToDefault(url, "/day"),
            "push-test-" + user.getId()
        );
        int sent = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        for (PushSubscriptionEntity subscription : subscriptions) {
            PushSendResult result = sendToSubscription(subscription, payload);
            if (result.success()) {
                sent++;
            } else {
                failed++;
                errors.add(result.message());
            }
        }
        return new PushTestResponse(subscriptions.size(), sent, failed, errors);
    }

    @Transactional
    public void sendToUser(AppUser user, PushPayload payload) {
        repository.findByUserIdAndActiveTrue(user.getId()).forEach(subscription -> sendToSubscription(subscription, payload));
    }

    public PushSendResult sendToSubscription(PushSubscriptionEntity subscription, PushPayload payload) {
        if (!hasVapidKeys()) {
            PushSendResult result = new PushSendResult(false, 0, "Push send failed: VAPID keys missing", false);
            log.warn("Push send skipped for {}: {}", endpointPreview(subscription.getEndpoint()), result.message());
            return result;
        }
        try {
            ensureBouncyCastle();
            String publicKey = normalizedVapidPublicKey();
            String privateKey = normalizedVapidPrivateKey();
            String subject = normalizedVapidSubject();
            String jsonPayload = objectMapper.writeValueAsString(payload);
            log.info(
                "Sending push: subject={}, publicKeyLen={}, privateKeyLen={}, endpointPrefix={}, p256dhLen={}, authLen={}, payload={}",
                subject,
                publicKey.length(),
                privateKey.length(),
                endpointPreview(subscription.getEndpoint()),
                safeLength(subscription.getP256dh()),
                safeLength(subscription.getAuth()),
                jsonPayload
            );
            Notification notification = new Notification(
                subscription.getEndpoint(),
                subscription.getP256dh(),
                subscription.getAuth(),
                jsonPayload.getBytes(StandardCharsets.UTF_8)
            );
            PushService pushService = new PushService(publicKey, privateKey, subject);
            HttpResponse response = pushService.send(notification, Encoding.AES128GCM);
            int status = response.getStatusLine().getStatusCode();
            String reason = response.getStatusLine().getReasonPhrase();
            String responseBody = responseBody(response);
            if (status == 404 || status == 410) {
                markInactive(subscription);
                String message = pushFailureMessage(status, reason, responseBody);
                log.warn("Push subscription expired for {}: {}", endpointPreview(subscription.getEndpoint()), message);
                return new PushSendResult(false, status, message, true);
            }
            if (status < 200 || status >= 300) {
                String message = pushFailureMessage(status, reason, responseBody);
                log.warn("Push send failed for {}: {}", endpointPreview(subscription.getEndpoint()), message);
                return new PushSendResult(false, status, message, false);
            }
            subscription.setLastUsedAt(Instant.now());
            repository.save(subscription);
            log.info("Push sent to {}: {} {}", endpointPreview(subscription.getEndpoint()), status, reason);
            return new PushSendResult(true, status, "Push sent: " + status + " " + reason, false);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid push payload");
        } catch (GeneralSecurityException | IOException | JoseException | InterruptedException | java.util.concurrent.ExecutionException | RuntimeException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            boolean expired = isExpiredSubscriptionError(ex);
            if (expired) {
                markInactive(subscription);
            }
            String message = "Push send failed: " + ex.getMessage();
            log.warn("Push send exception for {}: {}", endpointPreview(subscription.getEndpoint()), message, ex);
            return new PushSendResult(false, 0, message, expired);
        }
    }

    private boolean hasVapidKeys() {
        return !normalizedVapidPublicKey().isBlank()
            && !normalizedVapidPrivateKey().isBlank()
            && !normalizedVapidSubject().isBlank();
    }

    private void markInactive(PushSubscriptionEntity subscription) {
        subscription.setActive(false);
        repository.save(subscription);
    }

    private boolean isExpiredSubscriptionError(Exception ex) {
        String message = ex.getMessage();
        return message != null && (message.contains("404") || message.contains("410") || message.contains("Not Found") || message.contains("Gone"));
    }

    private void ensureBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String endpointPreview(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "<empty-endpoint>";
        }
        int visibleChars = Math.min(endpoint.length(), 80);
        return endpoint.substring(0, visibleChars) + (endpoint.length() > visibleChars ? "..." : "");
    }

    private String normalizedVapidPublicKey() {
        return normalizeConfigValue(vapidPublicKey);
    }

    private String normalizedVapidPrivateKey() {
        return normalizeConfigValue(vapidPrivateKey);
    }

    private String normalizedVapidSubject() {
        return normalizeConfigValue(vapidSubject);
    }

    private String normalizeConfigValue(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() >= 2
            && ((normalized.startsWith("\"") && normalized.endsWith("\""))
            || (normalized.startsWith("'") && normalized.endsWith("'")))) {
            return normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }

    private String responseBody(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return "";
        }
        return EntityUtils.toString(entity, StandardCharsets.UTF_8);
    }

    private String pushFailureMessage(int status, String reason, String responseBody) {
        String baseMessage = "Push send failed: " + status + " " + reason;
        if (responseBody == null || responseBody.isBlank()) {
            return baseMessage;
        }
        return baseMessage + " body: " + truncate(responseBody, 1000);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    public record PushSendResult(
        boolean success,
        int statusCode,
        String message,
        boolean expiredSubscription
    ) {
    }
}
