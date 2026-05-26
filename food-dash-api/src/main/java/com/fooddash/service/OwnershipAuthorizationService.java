package com.fooddash.service;

import com.fooddash.model.FoodOrder;
import com.fooddash.model.Restaurant;
import com.fooddash.model.Role;
import com.fooddash.model.User;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;

@Service
public class OwnershipAuthorizationService {

	public void assertCanManageRestaurant(User actor, Restaurant restaurant) {
		if (actor.getRole() == Role.ADMIN) {
			return;
		}
		if (actor.getRole() == Role.RESTAURANT_OWNER && restaurant.getOwner().getId().equals(actor.getId())) {
			return;
		}
		throw new AuthorizationDeniedException("Not allowed to modify this restaurant");
	}

	public void assertCanViewOrder(User actor, FoodOrder order) {
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
				throw new AuthorizationDeniedException("Delivery person assignment required");
			}
			if (order.getDeliveryPerson().getId().equals(actor.getId())) {
				return;
			}
		}
		throw new AuthorizationDeniedException("Not authorized for this order");
	}
}
