package com.fooddash.repository;

import com.fooddash.model.MenuItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

	Optional<MenuItem> findByIdAndRestaurantDeletedFalse(Long id);

	List<MenuItem> findByRestaurantIdAndRestaurantDeletedFalse(Long restaurantId);

	List<MenuItem> findByRestaurantIdAndRestaurantDeletedFalseAndCategoryIgnoreCase(Long restaurantId, String category);

	List<MenuItem> findByRestaurantIdAndRestaurantDeletedFalseAndAvailable(Long restaurantId, boolean available);

	List<MenuItem> findByRestaurantIdAndRestaurantDeletedFalseAndCategoryIgnoreCaseAndAvailable(
			Long restaurantId, String category, boolean available);
}
