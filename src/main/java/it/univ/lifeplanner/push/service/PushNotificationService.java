package it.univ.lifeplanner.push.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.univ.lifeplanner.common.BadRequestException;
import it.univ.lifeplanner.push.dto.PushPayload;
import it.univ.lifeplanner.push.dto.PushSubscriptionRequest;
import it.univ.lifeplanner.push.dto.PushSubscriptionResponse;
import it.univ.lifeplanner.push.model.PushSubscriptionEntity;
import it.univ.lifeplanner.push.repository.PushSubscriptionRepository;
import it.univ.lifeplanner.user.model.AppUser;
import it.univ.lifeplanner.user.service.CurrentUserService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
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
        return vapidPublicKey;
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
    public void sendTestToCurrentUser(String title, String body, String url) {
        AppUser user = currentUserService.requireCurrentUser();
        sendToUser(user, new PushPayload(
            blankToDefault(title, "Test LifePlanner"),
            blankToDefault(body, "Le notifiche funzionano"),
            blankToDefault(url, "/day"),
            "push-test-" + user.getId()
        ));
    }

    @Transactional
    public void sendToUser(AppUser user, PushPayload payload) {
        repository.findByUserIdAndActiveTrue(user.getId()).forEach(subscription -> sendToSubscription(subscription, payload));
    }

    public void sendToSubscription(PushSubscriptionEntity subscription, PushPayload payload) {
        if (!hasVapidKeys()) {
            return;
        }
        try {
            ensureBouncyCastle();
            String jsonPayload = objectMapper.writeValueAsString(payload);
            Notification notification = new Notification(subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth(), jsonPayload);
            PushService pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
            HttpResponse response = pushService.send(notification);
            int status = response.getStatusLine().getStatusCode();
            if (status == 404 || status == 410) {
                markInactive(subscription);
                return;
            }
            subscription.setLastUsedAt(Instant.now());
            repository.save(subscription);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid push payload");
        } catch (GeneralSecurityException | IOException | JoseException | InterruptedException | java.util.concurrent.ExecutionException | RuntimeException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (isExpiredSubscriptionError(ex)) {
                markInactive(subscription);
            }
        }
    }

    private boolean hasVapidKeys() {
        return vapidPublicKey != null && !vapidPublicKey.isBlank()
            && vapidPrivateKey != null && !vapidPrivateKey.isBlank();
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
}
