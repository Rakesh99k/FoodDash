package com.fooddash.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fooddash.model.Delivery;
import com.fooddash.model.FoodOrder;
import com.fooddash.model.MenuItem;
import com.fooddash.model.OrderStatus;
import com.fooddash.model.Restaurant;
import com.fooddash.model.Role;
import com.fooddash.model.User;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class OwnershipAuthorizationServiceTest {

	private OwnershipAuthorizationService ownershipAuthorizationService;

	@BeforeEach
	void setUp() {
		ownershipAuthorizationService = new OwnershipAuthorizationService();
	}

	@Test
	void restaurantOwnerCanManageOwnRestaurantAndMenu() {
		User owner = user(1L, Role.RESTAURANT_OWNER);
		Restaurant restaurant = restaurant(10L, owner);
		MenuItem menuItem = menuItem(20L, restaurant);

		assertDoesNotThrow(() -> ownershipAuthorizationService.verifyRestaurantOwnership(owner, restaurant));
		assertDoesNotThrow(() -> ownershipAuthorizationService.verifyMenuOwnership(owner, menuItem));
	}

	@Test
	void customerCannotManageRestaurant() {
		User customer = user(2L, Role.CUSTOMER);
		Restaurant restaurant = restaurant(10L, user(1L, Role.RESTAURANT_OWNER));

		assertThrows(
				AccessDeniedException.class,
				() -> ownershipAuthorizationService.verifyRestaurantOwnership(customer, restaurant));
	}

	@Test
	void deliveryPersonCanAccessAssignedDeliveryOnly() {
		User deliveryPerson = user(3L, Role.DELIVERY_PERSON);
		FoodOrder order = order(100L, user(2L, Role.CUSTOMER), restaurant(10L, user(1L, Role.RESTAURANT_OWNER)), deliveryPerson);
		Delivery delivery = Delivery.builder().order(order).deliveryPerson(deliveryPerson).trackingData(Map.of()).build();

		assertDoesNotThrow(() -> ownershipAuthorizationService.verifyOrderAccess(deliveryPerson, order));
		assertDoesNotThrow(() -> ownershipAuthorizationService.verifyDeliveryAccess(deliveryPerson, delivery));

		FoodOrder otherOrder = order(101L, user(2L, Role.CUSTOMER), restaurant(10L, user(1L, Role.RESTAURANT_OWNER)),
				user(99L, Role.DELIVERY_PERSON));
		Delivery otherDelivery = Delivery.builder()
				.order(otherOrder)
				.deliveryPerson(user(99L, Role.DELIVERY_PERSON))
				.trackingData(Map.of())
				.build();
		assertThrows(
				AccessDeniedException.class,
				() -> ownershipAuthorizationService.verifyDeliveryAccess(deliveryPerson, otherDelivery));
	}

	private User user(Long id, Role role) {
		return User.builder()
				.id(id)
				.role(role)
				.email(role.name().toLowerCase() + id + "@mail.com")
				.fullName("User " + id)
				.passwordHash("hash")
				.isActive(true)
				.build();
	}

	private Restaurant restaurant(Long id, User owner) {
		return Restaurant.builder()
				.id(id)
				.owner(owner)
				.name("Restaurant " + id)
				.address("Address")
				.cuisineType("Indian")
				.openingHours(Map.of())
				.status(com.fooddash.model.RestaurantStatus.ACTIVE)
				.build();
	}

	private MenuItem menuItem(Long id, Restaurant restaurant) {
		return MenuItem.builder()
				.id(id)
				.restaurant(restaurant)
				.name("Item " + id)
				.price(java.math.BigDecimal.TEN)
				.category("Main")
				.available(true)
				.build();
	}

	private FoodOrder order(Long id, User customer, Restaurant restaurant, User deliveryPerson) {
		return FoodOrder.builder()
				.id(id)
				.customer(customer)
				.restaurant(restaurant)
				.deliveryPerson(deliveryPerson)
				.status(OrderStatus.PENDING)
				.build();
	}
}
