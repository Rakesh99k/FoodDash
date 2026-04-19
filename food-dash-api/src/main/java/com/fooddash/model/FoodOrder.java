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
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User customer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "delivery_person_id")
	private User deliveryPerson;

	@Convert(converter = OrderStatusConverter.class)
	@Column(nullable = false)
	private OrderStatus status;

	@Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotalAmount;

	@Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal taxAmount;

	@Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
	private BigDecimal deliveryFee;

	@Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountAmount;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Column(columnDefinition = "TEXT")
	private String notes;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "delivery_address", nullable = false)
	private Map<String, Object> deliveryAddress;

	@Column(name = "placed_at", nullable = false)
	private Instant placedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
		this.placedAt = this.placedAt == null ? now : this.placedAt;
		this.status = this.status == null ? OrderStatus.PENDING : this.status;
		this.taxAmount = this.taxAmount == null ? BigDecimal.ZERO : this.taxAmount;
		this.deliveryFee = this.deliveryFee == null ? BigDecimal.ZERO : this.deliveryFee;
		this.discountAmount = this.discountAmount == null ? BigDecimal.ZERO : this.discountAmount;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = Instant.now();
	}
}
