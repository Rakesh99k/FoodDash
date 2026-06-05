package com.fooddash.service;

import com.fooddash.dto.CreateMenuItemRequest;
import com.fooddash.dto.MenuItemResponse;
import com.fooddash.dto.UpdateMenuItemRequest;
import com.fooddash.exception.ResourceNotFoundException;
import com.fooddash.model.MenuItem;
import com.fooddash.model.Restaurant;
import com.fooddash.model.User;
import com.fooddash.repository.MenuItemRepository;
import com.fooddash.repository.RestaurantRepository;
import com.fooddash.util.MenuItemMapper;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuItemService {

	private final MenuItemRepository menuItemRepository;
	private final RestaurantRepository restaurantRepository;
	private final AuthenticatedUserService authenticatedUserService;
	private final OwnershipAuthorizationService ownershipAuthorizationService;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	@Cacheable(
			cacheNames = "menuCache",
			key = "'menu:' + #restaurantId + ':' + (#category == null ? '' : #category.trim().toLowerCase()) + ':' + (#available == null ? 'na' : #available)")
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
	@CacheEvict(cacheNames = "menuCache", allEntries = true)
	public MenuItemResponse createMenuItem(Long restaurantId, CreateMenuItemRequest request) {
		Restaurant restaurant = findRestaurant(restaurantId);
		User actor = authenticatedUserService.getCurrentUser();
		ownershipAuthorizationService.verifyRestaurantOwnership(actor, restaurant);

		MenuItem menuItem = MenuItem.builder()
				.restaurant(restaurant)
				.name(request.getName().trim())
				.description(trimOrNull(request.getDescription()))
				.price(request.getPrice())
				.category(request.getCategory().trim())
				.imageUrl(trimOrNull(request.getImageUrl()))
				.available(request.getAvailable() == null || request.getAvailable())
				.build();

		MenuItem saved = menuItemRepository.save(menuItem);
		auditLogService.record(actor, "MENU_ITEM_CREATED", "MenuItem", saved.getId(), Map.of(
				"restaurantId", restaurant.getId(),
				"name", saved.getName(),
				"available", saved.isAvailable()));
		return MenuItemMapper.toResponse(saved);
	}

	@Transactional
	@CacheEvict(cacheNames = "menuCache", allEntries = true)
	public MenuItemResponse updateMenuItem(Long itemId, UpdateMenuItemRequest request) {
		MenuItem menuItem = findMenuItem(itemId);
		User actor = authenticatedUserService.getCurrentUser();
		ownershipAuthorizationService.verifyMenuOwnership(actor, menuItem);

		menuItem.setName(request.getName().trim());
		menuItem.setDescription(trimOrNull(request.getDescription()));
		menuItem.setPrice(request.getPrice());
		menuItem.setCategory(request.getCategory().trim());
		menuItem.setImageUrl(trimOrNull(request.getImageUrl()));
		menuItem.setAvailable(request.getAvailable());

		MenuItem saved = menuItemRepository.save(menuItem);
		auditLogService.record(actor, "MENU_ITEM_UPDATED", "MenuItem", saved.getId(), Map.of(
				"restaurantId", saved.getRestaurant().getId(),
				"name", saved.getName(),
				"available", saved.isAvailable()));
		return MenuItemMapper.toResponse(saved);
	}

	@Transactional
	@CacheEvict(cacheNames = "menuCache", allEntries = true)
	public MenuItemResponse softDeleteMenuItem(Long itemId) {
		MenuItem menuItem = findMenuItem(itemId);
		User actor = authenticatedUserService.getCurrentUser();
		ownershipAuthorizationService.verifyMenuOwnership(actor, menuItem);

		menuItem.setAvailable(false);
		MenuItem saved = menuItemRepository.save(menuItem);
		auditLogService.record(actor, "MENU_ITEM_DELETED", "MenuItem", saved.getId(), Map.of(
				"restaurantId", saved.getRestaurant().getId(),
				"available", saved.isAvailable()));
		return MenuItemMapper.toResponse(saved);
	}

	private MenuItem findMenuItem(Long itemId) {
		return menuItemRepository.findByIdAndRestaurantDeletedFalse(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemId));
	}

	private Restaurant findRestaurant(Long restaurantId) {
		return restaurantRepository.findByIdAndDeletedFalse(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));
	}

	private String trimOrNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
