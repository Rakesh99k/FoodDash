package com.fooddash.repository;

import com.fooddash.model.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}
