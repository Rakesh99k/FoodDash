package com.fooddash.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fooddash.model.OrderStatus;
import com.fooddash.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class OrderStateMachineServiceTest {

	private OrderStateMachineService orderStateMachineService;

	@BeforeEach
	void setUp() {
		orderStateMachineService = new OrderStateMachineService();
	}

	@Test
	void allowsForwardTransition() {
		assertDoesNotThrow(
				() -> orderStateMachineService.assertTransitionAllowed(OrderStatus.PENDING, OrderStatus.CONFIRMED));
	}

	@Test
	void blocksBackwardTransition() {
		assertThrows(
				IllegalStateException.class,
				() -> orderStateMachineService.assertTransitionAllowed(OrderStatus.DELIVERED, OrderStatus.PREPARING));
	}

	@Test
	void customerCannotConfirmOrder() {
		assertThrows(
				AccessDeniedException.class,
				() -> orderStateMachineService.assertRoleCanTransition(
						Role.CUSTOMER, OrderStatus.PENDING, OrderStatus.CONFIRMED));
	}
}
