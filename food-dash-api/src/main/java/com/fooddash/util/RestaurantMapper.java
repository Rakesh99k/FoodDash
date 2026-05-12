package com.fooddash.util;

import com.fooddash.dto.RestaurantResponse;
import com.fooddash.model.Restaurant;

public final class RestaurantMapper {

	private RestaurantMapper() {
	}

	public static RestaurantResponse toResponse(Restaurant restaurant) {
		return RestaurantResponse.builder()
				.id(restaurant.getId())
				.ownerId(restaurant.getOwner().getId())
				.name(restaurant.getName())
				.description(restaurant.getDescription())
				.address(restaurant.getAddress())
				.latitude(restaurant.getLatitude())
				.longitude(restaurant.getLongitude())
				.cuisineType(restaurant.getCuisineType())
				.openingHours(restaurant.getOpeningHours())
				.status(restaurant.getStatus())
				.createdAt(restaurant.getCreatedAt())
				.updatedAt(restaurant.getUpdatedAt())
				.build();
	}
}
