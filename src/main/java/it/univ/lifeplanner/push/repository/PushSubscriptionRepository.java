package it.univ.lifeplanner.push.repository;

import it.univ.lifeplanner.push.model.PushSubscriptionEntity;
import it.univ.lifeplanner.user.model.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, Long> {
    Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);

    List<PushSubscriptionEntity> findByUserAndActiveTrue(AppUser user);

    List<PushSubscriptionEntity> findByUserIdAndActiveTrue(Long userId);

    List<PushSubscriptionEntity> findAllByActiveTrue();
}
