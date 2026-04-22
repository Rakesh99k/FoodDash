package com.fooddash.service;

import com.fooddash.dto.CreateMenuItemRequest;
import com.fooddash.dto.MenuItemResponse;
import com.fooddash.dto.UpdateMenuItemRequest;
import com.fooddash.exception.ResourceNotFoundException;
import com.fooddash.model.MenuItem;
import com.fooddash.model.Restaurant;
import com.fooddash.model.Role;
import com.fooddash.model.User;
import com.fooddash.repository.MenuItemRepository;
import com.fooddash.repository.RestaurantRepository;
import com.fooddash.util.MenuItemMapper;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuItemService {

	private final MenuItemRepository menuItemRepository;
	private final RestaurantRepository restaurantRepository;
	private final CustomUserDetailsService customUserDetailsService;

	@Transactional(readOnly = true)
	public List<MenuItemResponse> listMenuItems(Long restaurantId, @Nullable String category, @Nullable Boolean available) {
		Restaurant restaurant = findRestaurant(restaurantId);
		List<MenuItem> menuItems;

		if (category != null && !category.isBlank() && available != null) {
			menuItems = menuItemRepository.findByRestaurantIdAndRestaurantDeletedFalseAndCategoryIgnoreCaseAndAvailable(
					restaurant.getId(), category.trim(), available);
		}
		else if (category != null && !category.isBlank()) {
			menuItems = menuItemRepository.findByRestaurantIdAndRestaurantDeletedFalseAndCategoryIgnoreCase(
					restaurant.getId(), category.trim());
		}
		else if (available != null) {
			menuItems = menuItemRepository.findByRestaurantIdAndRestaurantDeletedFalseAndAvailable(
					restaurant.getId(), available);
		}
		else {
			menuItems = menuItemRepository.findByRestaurantIdAndRestaurantDeletedFalse(restaurant.getId());
		}

		return menuItems.stream().map(MenuItemMapper::toResponse).toList();
	}

	@Transactional
	public MenuItemResponse createMenuItem(Long restaurantId, CreateMenuItemRequest request) {
		Restaurant restaurant = findRestaurant(restaurantId);
		User actor = getCurrentUser();
		validateCanModify(actor, restaurant);

		MenuItem menuItem = MenuItem.builder()
				.restaurant(restaurant)
				.name(request.getName().trim())
				.description(trimOrNull(request.getDescription()))
				.price(request.getPrice())
				.category(request.getCategory().trim())
				.imageUrl(trimOrNull(request.getImageUrl()))
				.available(request.getAvailable() == null || request.getAvailable())
				.build();

		return MenuItemMapper.toResponse(menuItemRepository.save(menuItem));
	}

	@Transactional
	public MenuItemResponse updateMenuItem(Long itemId, UpdateMenuItemRequest request) {
		MenuItem menuItem = findMenuItem(itemId);
		User actor = getCurrentUser();
		validateCanModify(actor, menuItem.getRestaurant());

		menuItem.setName(request.getName().trim());
		menuItem.setDescription(trimOrNull(request.getDescription()));
		menuItem.setPrice(request.getPrice());
		menuItem.setCategory(request.getCategory().trim());
		menuItem.setImageUrl(trimOrNull(request.getImageUrl()));
		menuItem.setAvailable(request.getAvailable());

		return MenuItemMapper.toResponse(menuItemRepository.save(menuItem));
	}

	@Transactional
	public MenuItemResponse softDeleteMenuItem(Long itemId) {
		MenuItem menuItem = findMenuItem(itemId);
		User actor = getCurrentUser();
		validateCanModify(actor, menuItem.getRestaurant());

		menuItem.setAvailable(false);
		return MenuItemMapper.toResponse(menuItemRepository.save(menuItem));
	}

	private MenuItem findMenuItem(Long itemId) {
		return menuItemRepository.findByIdAndRestaurantDeletedFalse(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemId));
	}

	private Restaurant findRestaurant(Long restaurantId) {
		return restaurantRepository.findByIdAndDeletedFalse(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));
	}

	private User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AuthorizationDeniedException("Unauthenticated");
		}
		return customUserDetailsService.loadDomainUserByEmail(authentication.getName());
	}

	private void validateCanModify(User actor, Restaurant restaurant) {
		if (actor.getRole() == Role.ADMIN) {
			return;
		}
		if (actor.getRole() == Role.RESTAURANT_OWNER && restaurant.getOwner().getId().equals(actor.getId())) {
			return;
		}
		throw new AuthorizationDeniedException("Only restaurant owner or admin can modify menu items");
	}

	private String trimOrNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
