package com.fooddash.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MenuItemResponse {

	private Long id;
	private Long restaurantId;
	private String name;
	private String description;
	private BigDecimal price;
	private String category;
	private String imageUrl;
	private boolean available;
	private Instant createdAt;
}
