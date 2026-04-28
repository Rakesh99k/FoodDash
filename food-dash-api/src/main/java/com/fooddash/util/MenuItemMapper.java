package com.fooddash.util;

import com.fooddash.dto.MenuItemResponse;
import com.fooddash.model.MenuItem;

public final class MenuItemMapper {

	private MenuItemMapper() {
	}

	public static MenuItemResponse toResponse(MenuItem menuItem) {
		return MenuItemResponse.builder()
				.id(menuItem.getId())
				.restaurantId(menuItem.getRestaurant().getId())
				.name(menuItem.getName())
				.description(menuItem.getDescription())
				.price(menuItem.getPrice())
				.category(menuItem.getCategory())
				.imageUrl(menuItem.getImageUrl())
				.available(menuItem.isAvailable())
				.createdAt(menuItem.getCreatedAt())
				.build();
	}
}
