package com.fooddash.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "webhook_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String provider;

	@Column(name = "event_id", nullable = false, length = 120)
	private String eventId;

	@Column(name = "event_type", nullable = false, length = 120)
	private String eventType;

	@Column(columnDefinition = "TEXT")
	private String payload;

	@Column(nullable = false, length = 30)
	private String status;

	@Column(name = "processed_at")
	private Instant processedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
		if (this.status == null || this.status.isBlank()) {
			this.status = "RECEIVED";
		}
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = Instant.now();
	}
}
