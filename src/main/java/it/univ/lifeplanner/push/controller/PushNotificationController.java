package it.univ.lifeplanner.push.controller;

import it.univ.lifeplanner.push.dto.PushSubscriptionRequest;
import it.univ.lifeplanner.push.dto.PushSubscriptionResponse;
import it.univ.lifeplanner.push.dto.PushTestRequest;
import it.univ.lifeplanner.push.dto.PushTestResponse;
import it.univ.lifeplanner.push.service.PushNotificationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushNotificationController {
    private final PushNotificationService service;

    @GetMapping("/vapid-public-key")
    public Map<String, String> getVapidPublicKey() {
        return Map.of("publicKey", service.getPublicKey());
    }

    @PostMapping("/subscriptions")
    public PushSubscriptionResponse saveSubscription(@Valid @RequestBody PushSubscriptionRequest request) {
        return service.saveForCurrentUser(request);
    }

    @GetMapping("/subscriptions")
    public List<PushSubscriptionResponse> findSubscriptions() {
        return service.findCurrentUserSubscriptions();
    }

    @DeleteMapping("/subscriptions")
    public void deleteSubscription(@RequestBody Map<String, String> request) {
        service.deactivateForCurrentUser(request.get("endpoint"));
    }

    @PostMapping("/test")
    public PushTestResponse sendTest(@Valid @RequestBody(required = false) PushTestRequest request) {
        return service.sendTestToCurrentUser(
            request == null ? null : request.title(),
            request == null ? null : request.body(),
            request == null ? null : request.url()
        );
    }
}
