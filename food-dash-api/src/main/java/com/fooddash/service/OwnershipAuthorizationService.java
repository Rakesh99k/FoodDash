package com.fooddash.service;

import com.fooddash.model.Delivery;
import com.fooddash.model.FoodOrder;
import com.fooddash.model.MenuItem;
import com.fooddash.model.Restaurant;
import com.fooddash.model.Role;
import com.fooddash.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class OwnershipAuthorizationService {

	public void verifyRestaurantOwnership(User actor, Restaurant restaurant) {
		assertActorPresent(actor);
		if (actor.getRole() == Role.ADMIN) {
			return;
		}
		if (actor.getRole() == Role.RESTAURANT_OWNER && restaurant.getOwner().getId().equals(actor.getId())) {
			return;
		}
		throw new AccessDeniedException("Not allowed to modify this restaurant");
	}

	public void verifyMenuOwnership(User actor, MenuItem menuItem) {
		verifyRestaurantOwnership(actor, menuItem.getRestaurant());
	}

	public void verifyOrderAccess(User actor, FoodOrder order) {
		assertActorPresent(actor);
		if (actor.getRole() == Role.ADMIN) {
			return;
		}
		if (actor.getRole() == Role.CUSTOMER && order.getCustomer().getId().equals(actor.getId())) {
			return;
		}
		if (actor.getRole() == Role.RESTAURANT_OWNER && order.getRestaurant().getOwner().getId().equals(actor.getId())) {
			return;
		}
		if (actor.getRole() == Role.DELIVERY_PERSON) {
			if (order.getDeliveryPerson() == null) {
				throw new AccessDeniedException("Delivery person assignment required");
			}
			if (order.getDeliveryPerson().getId().equals(actor.getId())) {
				return;
			}
		}
		throw new AccessDeniedException("Not authorized for this order");
	}

	public void verifyDeliveryAccess(User actor, Delivery delivery) {
		assertActorPresent(actor);
		if (actor.getRole() == Role.ADMIN) {
			return;
		}
		if (delivery.getDeliveryPerson() != null && delivery.getDeliveryPerson().getId().equals(actor.getId())) {
			return;
		}
		if (delivery.getOrder() != null) {
			verifyOrderAccess(actor, delivery.getOrder());
			return;
		}
		throw new AccessDeniedException("Not authorized for this delivery");
	}

	public void assertCanManageRestaurant(User actor, Restaurant restaurant) {
		verifyRestaurantOwnership(actor, restaurant);
	}

	public void assertCanViewOrder(User actor, FoodOrder order) {
		verifyOrderAccess(actor, order);
	}

	private void assertActorPresent(User actor) {
		if (actor == null) {
			throw new AccessDeniedException("Unauthenticated");
		}
	}
}
