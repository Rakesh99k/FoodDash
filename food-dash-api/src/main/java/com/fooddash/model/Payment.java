package com.fooddash.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private FoodOrder order;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Convert(converter = PaymentMethodConverter.class)
	@Column(nullable = false, length = 20)
	private PaymentMethod method;

	@Convert(converter = PaymentStatusConverter.class)
	@Column(nullable = false, length = 20)
	private PaymentStatus status;

	@Convert(converter = PaymentProviderConverter.class)
	@Column(nullable = false, length = 50)
	private PaymentProvider provider;

	@Column(name = "provider_payment_id", length = 100)
	private String providerPaymentId;

	@Column(name = "provider_order_id", length = 120)
	private String providerOrderId;

	@Column(name = "provider_reference", length = 255)
	private String providerReference;

	@Column(name = "provider_client_secret", length = 255)
	private String providerClientSecret;

	@Column(name = "webhook_event_id", length = 120)
	private String webhookEventId;

	@Column(name = "failure_reason", columnDefinition = "TEXT")
	private String failureReason;

	@Column(name = "paid_at")
	private Instant paidAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
		if (this.status == null) {
			this.status = PaymentStatus.PENDING;
		}
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = Instant.now();
	}
}
