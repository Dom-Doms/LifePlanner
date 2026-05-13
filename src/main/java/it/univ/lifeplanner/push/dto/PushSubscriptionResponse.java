package it.univ.lifeplanner.push.dto;

import it.univ.lifeplanner.push.model.PushSubscriptionEntity;
import java.time.Instant;

public record PushSubscriptionResponse(
    Long id,
    String endpoint,
    boolean active,
    Instant createdAt
) {
    public static PushSubscriptionResponse from(PushSubscriptionEntity subscription) {
        return new PushSubscriptionResponse(
            subscription.getId(),
            subscription.getEndpoint(),
            subscription.isActive(),
            subscription.getCreatedAt()
        );
    }
}
