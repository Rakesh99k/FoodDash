package com.fooddash.repository;

import com.fooddash.model.Payment;
import com.fooddash.model.PaymentProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

	Optional<Payment> findByProviderAndProviderPaymentId(PaymentProvider provider, String providerPaymentId);

	Optional<Payment> findByProviderAndProviderOrderId(PaymentProvider provider, String providerOrderId);

	boolean existsByWebhookEventId(String webhookEventId);
}
