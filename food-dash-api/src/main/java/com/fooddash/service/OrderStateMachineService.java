package com.fooddash.service;

import com.fooddash.model.OrderStatus;
import com.fooddash.model.Role;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;

@Service
public class OrderStateMachineService {

	private final Map<OrderStatus, Set<OrderStatus>> allowedTransitions = buildAllowedTransitions();

	public void assertTransitionAllowed(OrderStatus from, OrderStatus to) {
		if (from == to) {
			return;
		}
		Set<OrderStatus> targets = allowedTransitions.getOrDefault(from, Set.of());
		if (!targets.contains(to)) {
			throw new IllegalArgumentException("Invalid order status transition: " + from + " -> " + to);
		}
	}

	public void assertRoleCanTransition(Role role, OrderStatus from, OrderStatus to) {
		if (role == Role.ADMIN) {
			return;
		}
		switch (role) {
			case CUSTOMER -> {
				if (to != OrderStatus.CANCELLED
						|| (from != OrderStatus.PENDING && from != OrderStatus.CONFIRMED)) {
					throw new AuthorizationDeniedException("Customers can only cancel pending/confirmed orders");
				}
			}
			case RESTAURANT_OWNER -> {
				if (!(to == OrderStatus.CONFIRMED
						|| to == OrderStatus.PREPARING
						|| to == OrderStatus.READY_FOR_PICKUP
						|| to == OrderStatus.CANCELLED)) {
					throw new AuthorizationDeniedException("Invalid status update for restaurant owner");
				}
			}
			case DELIVERY_PERSON -> {
				if (!(to == OrderStatus.OUT_FOR_DELIVERY || to == OrderStatus.DELIVERED)) {
					throw new AuthorizationDeniedException("Invalid status update for delivery person");
				}
			}
			default -> throw new AuthorizationDeniedException("Unsupported role for status updates");
		}
	}

	private Map<OrderStatus, Set<OrderStatus>> buildAllowedTransitions() {
		Map<OrderStatus, Set<OrderStatus>> transitions = new EnumMap<>(OrderStatus.class);
		transitions.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
		transitions.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
		transitions.put(OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.CANCELLED));
		transitions.put(OrderStatus.READY_FOR_PICKUP, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY));
		transitions.put(OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED));
		transitions.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
		transitions.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
		return transitions;
	}
}
