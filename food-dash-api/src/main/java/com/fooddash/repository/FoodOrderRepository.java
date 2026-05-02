package com.fooddash.repository;

import com.fooddash.model.FoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

    List<FoodOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<FoodOrder> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);

    List<FoodOrder> findByDeliveryPersonIdOrderByCreatedAtDesc(Long deliveryPersonId);
}
