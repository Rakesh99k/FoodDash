package com.fooddash.controller;

import com.fooddash.dto.ApiResponse;
import com.fooddash.dto.CreateRestaurantRequest;
import com.fooddash.dto.RestaurantResponse;
import com.fooddash.dto.UpdateRestaurantRequest;
import com.fooddash.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

	private final RestaurantService restaurantService;

	@GetMapping
	public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> listRestaurants(
			@RequestParam(required = false) String cuisine,
			@RequestParam(required = false) Double latitude,
			@RequestParam(required = false) Double longitude,
			@RequestParam(required = false) Double radiusKm,
			@PageableDefault(size = 20) Pageable pageable) {
		Page<RestaurantResponse> restaurants =
				restaurantService.listPublicRestaurants(cuisine, latitude, longitude, radiusKm, pageable);
		return ResponseEntity.ok(ApiResponse.success("Restaurants fetched", restaurants));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<RestaurantResponse>> getRestaurant(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("Restaurant fetched", restaurantService.getPublicRestaurantById(id)));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
	public ResponseEntity<ApiResponse<RestaurantResponse>> createRestaurant(@Valid @RequestBody CreateRestaurantRequest request) {
		RestaurantResponse created = restaurantService.createRestaurant(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Restaurant created", created));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
	public ResponseEntity<ApiResponse<RestaurantResponse>> updateRestaurant(
			@PathVariable Long id, @Valid @RequestBody UpdateRestaurantRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Restaurant updated", restaurantService.updateRestaurant(id, request)));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<RestaurantResponse>> deleteRestaurant(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("Restaurant deleted", restaurantService.softDeleteRestaurant(id)));
	}
}
