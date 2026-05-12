package com.fooddash.repository;

import com.fooddash.model.Restaurant;
import com.fooddash.model.RestaurantStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

	Optional<Restaurant> findByIdAndDeletedFalse(Long id);

	List<Restaurant> findByDeletedFalseAndStatus(RestaurantStatus status);

	List<Restaurant> findByDeletedFalseAndStatusAndCuisineTypeIgnoreCase(RestaurantStatus status, String cuisineType);

	List<Restaurant> findByOwnerId(Long ownerId);
}
