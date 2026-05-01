package com.fooddash.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_user_id", nullable = false)
	private User owner;

	@Column(nullable = false)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private String address;

	@Column(name = "location_lat")
	private Double latitude;

	@Column(name = "location_lng")
	private Double longitude;

	@Column(name = "cuisine_type", nullable = false)
	private String cuisineType;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "opening_hours", nullable = false)
	private Map<String, Object> openingHours;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RestaurantStatus status;

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private boolean deleted = false;

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
			this.status = RestaurantStatus.ACTIVE;
		}
		if (this.openingHours == null) {
			this.openingHours = Map.of();
		}
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = Instant.now();
	}
}
