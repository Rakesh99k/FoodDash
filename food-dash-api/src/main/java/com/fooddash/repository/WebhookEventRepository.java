package com.fooddash.repository;

import com.fooddash.model.WebhookEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

	boolean existsByProviderAndEventId(String provider, String eventId);

	Optional<WebhookEvent> findByProviderAndEventId(String provider, String eventId);
}
