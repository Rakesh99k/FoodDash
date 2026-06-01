package com.fooddash.repository;

import com.fooddash.model.Delivery;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

	Optional<Delivery> findByOrderId(Long orderId);
}
