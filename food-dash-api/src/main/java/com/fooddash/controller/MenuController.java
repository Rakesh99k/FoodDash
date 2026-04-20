package com.fooddash.controller;

import com.fooddash.dto.ApiResponse;
import com.fooddash.dto.CreateMenuItemRequest;
import com.fooddash.dto.MenuItemResponse;
import com.fooddash.dto.UpdateMenuItemRequest;
import com.fooddash.service.MenuItemService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MenuController {

	private final MenuItemService menuItemService;

	@GetMapping("/restaurants/{restaurantId}/menu")
	public ResponseEntity<ApiResponse<List<MenuItemResponse>>> listMenuItems(
			@PathVariable Long restaurantId,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) Boolean available) {
		List<MenuItemResponse> items = menuItemService.listMenuItems(restaurantId, category, available);
		return ResponseEntity.ok(ApiResponse.success("Menu items fetched", items));
	}

	@PostMapping("/restaurants/{restaurantId}/menu")
	@PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
	public ResponseEntity<ApiResponse<MenuItemResponse>> createMenuItem(
			@PathVariable Long restaurantId, @Valid @RequestBody CreateMenuItemRequest request) {
		MenuItemResponse created = menuItemService.createMenuItem(restaurantId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Menu item created", created));
	}

	@PutMapping("/menu/{itemId}")
	@PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
	public ResponseEntity<ApiResponse<MenuItemResponse>> updateMenuItem(
			@PathVariable Long itemId, @Valid @RequestBody UpdateMenuItemRequest request) {
		MenuItemResponse updated = menuItemService.updateMenuItem(itemId, request);
		return ResponseEntity.ok(ApiResponse.success("Menu item updated", updated));
	}

	@DeleteMapping("/menu/{itemId}")
	@PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
	public ResponseEntity<ApiResponse<MenuItemResponse>> deleteMenuItem(@PathVariable Long itemId) {
		MenuItemResponse deleted = menuItemService.softDeleteMenuItem(itemId);
		return ResponseEntity.ok(ApiResponse.success("Menu item deleted", deleted));
	}
}
