package com.fooddash.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private FoodOrder order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "delivery_person_id")
	private User deliveryPerson;

	@Column(nullable = false)
	private String status;

	@Column(name = "accepted_at")
	private Instant acceptedAt;

	@Column(name = "picked_up_at")
	private Instant pickedUpAt;

	@Column(name = "delivered_at")
	private Instant deliveredAt;

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
			this.status = "assigned";
		}
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = Instant.now();
	}
}
