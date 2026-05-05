package com.fooddash.dto;

import com.fooddash.model.RestaurantStatus;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RestaurantResponse {

	private Long id;
	private Long ownerId;
	private String name;
	private String description;
	private String address;
	private Double latitude;
	private Double longitude;
	private String cuisineType;
	private Map<String, Object> openingHours;
	private RestaurantStatus status;
	private Instant createdAt;
	private Instant updatedAt;
}
