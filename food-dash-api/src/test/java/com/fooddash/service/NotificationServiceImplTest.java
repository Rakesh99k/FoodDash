package com.fooddash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fooddash.model.FoodOrder;
import com.fooddash.model.Notification;
import com.fooddash.model.OrderStatus;
import com.fooddash.model.Restaurant;
import com.fooddash.model.RestaurantStatus;
import com.fooddash.model.User;
import com.fooddash.repository.NotificationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class NotificationServiceImplTest {

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@Mock
	private NotificationRepository notificationRepository;

	private NotificationServiceImpl notificationService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		notificationService = new NotificationServiceImpl(messagingTemplate, notificationRepository);
	}

	@Test
	void notifyOrderConfirmedPersistsAndPushesUserNotification() {
		User customer = user(1L, "customer@mail.com");
		FoodOrder order = order(10L, customer, restaurant(20L));
		when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
			Notification notification = invocation.getArgument(0);
			notification.setId(99L);
			return notification;
		});

		notificationService.notifyOrderConfirmed(order);

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(captor.capture());
		verify(messagingTemplate).convertAndSendToUser(eq("customer@mail.com"), eq("/queue/notifications"), any());
		Notification saved = captor.getValue();
		assertEquals("ORDER_CONFIRMED", saved.getType());
		assertEquals("sent", saved.getStatus());
		assertEquals(10L, saved.getOrder().getId());
	}

	private User user(Long id, String email) {
		return User.builder()
				.id(id)
				.email(email)
				.fullName("User")
				.passwordHash("hash")
				.role(com.fooddash.model.Role.CUSTOMER)
				.isActive(true)
				.build();
	}

	private Restaurant restaurant(Long id) {
		return Restaurant.builder()
				.id(id)
				.name("Restaurant")
				.address("Address")
				.cuisineType("Indian")
				.openingHours(Map.of())
				.status(RestaurantStatus.ACTIVE)
				.owner(user(2L, "owner@mail.com"))
				.build();
	}

	private FoodOrder order(Long id, User customer, Restaurant restaurant) {
		return FoodOrder.builder()
				.id(id)
				.customer(customer)
				.restaurant(restaurant)
				.status(OrderStatus.CONFIRMED)
				.subtotalAmount(BigDecimal.TEN)
				.taxAmount(BigDecimal.ONE)
				.deliveryFee(BigDecimal.ONE)
				.discountAmount(BigDecimal.ZERO)
				.totalAmount(BigDecimal.valueOf(12))
				.deliveryAddress(Map.of("line1", "Address"))
				.placedAt(Instant.now())
				.build();
	}
}
