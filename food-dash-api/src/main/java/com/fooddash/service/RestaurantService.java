package com.fooddash.service;

import com.fooddash.dto.CreateRestaurantRequest;
import com.fooddash.dto.RestaurantResponse;
import com.fooddash.dto.UpdateRestaurantRequest;
import com.fooddash.exception.ResourceNotFoundException;
import com.fooddash.model.Restaurant;
import com.fooddash.model.RestaurantStatus;
import com.fooddash.model.Role;
import com.fooddash.model.User;
import com.fooddash.repository.RestaurantRepository;
import com.fooddash.repository.UserRepository;
import com.fooddash.util.RestaurantMapper;
import jakarta.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantService {

	private final RestaurantRepository restaurantRepository;
	private final UserRepository userRepository;
	private final AuthenticatedUserService authenticatedUserService;
	private final OwnershipAuthorizationService ownershipAuthorizationService;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	@Cacheable(
			cacheNames = "restaurantCache",
			key = "'list:' + (#cuisine == null ? '' : #cuisine.trim().toLowerCase()) + ':' + (#latitude == null ? 'na' : #latitude) + ':' + (#longitude == null ? 'na' : #longitude) + ':' + (#radiusKm == null ? 'na' : #radiusKm) + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
	public Page<RestaurantResponse> listPublicRestaurants(
			@Nullable String cuisine, @Nullable Double latitude, @Nullable Double longitude, @Nullable Double radiusKm, Pageable pageable) {
		List<Restaurant> base = cuisine == null || cuisine.isBlank()
				? restaurantRepository.findByDeletedFalseAndStatus(RestaurantStatus.ACTIVE)
				: restaurantRepository.findByDeletedFalseAndStatusAndCuisineTypeIgnoreCase(
						RestaurantStatus.ACTIVE, cuisine.trim());

		List<Restaurant> filtered = base.stream()
				.filter(r -> isWithinRadius(r, latitude, longitude, radiusKm))
				.sorted(Comparator.comparing(Restaurant::getCreatedAt).reversed())
				.toList();

		int start = (int) pageable.getOffset();
		int end = Math.min(start + pageable.getPageSize(), filtered.size());
		List<RestaurantResponse> content = start >= filtered.size()
				? List.of()
				: filtered.subList(start, end).stream().map(RestaurantMapper::toResponse).toList();
		return new PageImpl<>(content, pageable, filtered.size());
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = "restaurantCache", key = "'public:' + #id")
	public RestaurantResponse getPublicRestaurantById(Long id) {
		Restaurant restaurant = findRestaurant(id);
		if (restaurant.getStatus() != RestaurantStatus.ACTIVE) {
			throw new ResourceNotFoundException("Restaurant not found: " + id);
		}
		return RestaurantMapper.toResponse(restaurant);
	}

	@Transactional
	@CacheEvict(cacheNames = "restaurantCache", allEntries = true)
	public RestaurantResponse createRestaurant(CreateRestaurantRequest request) {
		User actor = authenticatedUserService.getCurrentUser();
		boolean admin = actor.getRole() == Role.ADMIN;
		boolean owner = actor.getRole() == Role.RESTAURANT_OWNER;
		if (!admin && !owner) {
			throw new AuthorizationDeniedException("Only restaurant owners or admins can create restaurants");
		}

		Long targetOwnerId = request.getOwnerId();
		User ownerUser;
		if (admin && targetOwnerId != null) {
			ownerUser = findUser(targetOwnerId);
		}
		else {
			ownerUser = actor;
		}

		Restaurant restaurant = Restaurant.builder()
				.owner(ownerUser)
				.name(request.getName().trim())
				.description(trimOrNull(request.getDescription()))
				.address(request.getAddress().trim())
				.latitude(request.getLatitude())
				.longitude(request.getLongitude())
				.cuisineType(request.getCuisineType().trim())
				.openingHours(request.getOpeningHours() == null ? Map.of() : request.getOpeningHours())
				.status(request.getStatus() == null ? RestaurantStatus.ACTIVE : request.getStatus())
				.deleted(false)
				.build();

		Restaurant saved = restaurantRepository.save(restaurant);
		auditLogService.record(actor, "RESTAURANT_CREATED", "Restaurant", saved.getId(), Map.of(
				"ownerId", saved.getOwner().getId(),
				"name", saved.getName(),
				"status", saved.getStatus().name()));
		return RestaurantMapper.toResponse(saved);
	}

	@Transactional
	@CacheEvict(cacheNames = "restaurantCache", allEntries = true)
	public RestaurantResponse updateRestaurant(Long id, UpdateRestaurantRequest request) {
		User actor = authenticatedUserService.getCurrentUser();
		Restaurant restaurant = findRestaurant(id);
		ownershipAuthorizationService.verifyRestaurantOwnership(actor, restaurant);

		if (actor.getRole() == Role.ADMIN && request.getOwnerId() != null) {
			restaurant.setOwner(findUser(request.getOwnerId()));
		}

		restaurant.setName(request.getName().trim());
		restaurant.setDescription(trimOrNull(request.getDescription()));
		restaurant.setAddress(request.getAddress().trim());
		restaurant.setLatitude(request.getLatitude());
		restaurant.setLongitude(request.getLongitude());
		restaurant.setCuisineType(request.getCuisineType().trim());
		restaurant.setOpeningHours(request.getOpeningHours() == null ? Map.of() : request.getOpeningHours());
		if (request.getStatus() != null) {
			restaurant.setStatus(request.getStatus());
		}

		Restaurant saved = restaurantRepository.save(restaurant);
		auditLogService.record(actor, "RESTAURANT_UPDATED", "Restaurant", saved.getId(), Map.of(
				"name", saved.getName(),
				"status", saved.getStatus().name(),
				"ownerId", saved.getOwner().getId()));
		return RestaurantMapper.toResponse(saved);
	}

	@Transactional
	@CacheEvict(cacheNames = "restaurantCache", allEntries = true)
	public RestaurantResponse softDeleteRestaurant(Long id) {
		User actor = authenticatedUserService.getCurrentUser();
		if (actor.getRole() != Role.ADMIN) {
			throw new AuthorizationDeniedException("Only admins can delete restaurants");
		}

		Restaurant restaurant = findRestaurant(id);
		restaurant.setDeleted(true);
		restaurant.setStatus(RestaurantStatus.CLOSED);
		Restaurant saved = restaurantRepository.save(restaurant);
		auditLogService.record(actor, "RESTAURANT_DELETED", "Restaurant", saved.getId(), Map.of(
				"deleted", true,
				"status", saved.getStatus().name()));
		return RestaurantMapper.toResponse(saved);
	}

	private Restaurant findRestaurant(Long id) {
		return restaurantRepository.findByIdAndDeletedFalse(id)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + id));
	}

	private User findUser(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
	}

	private boolean isWithinRadius(
			Restaurant restaurant, @Nullable Double latitude, @Nullable Double longitude, @Nullable Double radiusKm) {
		if (latitude == null || longitude == null || radiusKm == null) {
			return true;
		}
		if (restaurant.getLatitude() == null || restaurant.getLongitude() == null) {
			return false;
		}
		return haversineKm(latitude, longitude, restaurant.getLatitude(), restaurant.getLongitude()) <= radiusKm;
	}

	private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
		double earthRadiusKm = 6371.0;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1))
						* Math.cos(Math.toRadians(lat2))
						* Math.sin(dLon / 2)
						* Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return earthRadiusKm * c;
	}

	private String trimOrNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
