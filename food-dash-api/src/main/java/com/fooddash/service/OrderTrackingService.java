package com.fooddash.service;

import com.fooddash.dto.OrderTrackingResponse;
import com.fooddash.dto.OrderTrackingUpdateRequest;
import com.fooddash.exception.ResourceNotFoundException;
import com.fooddash.model.Delivery;
import com.fooddash.model.FoodOrder;
import com.fooddash.model.Role;
import com.fooddash.model.User;
import com.fooddash.repository.DeliveryRepository;
import com.fooddash.repository.FoodOrderRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderTrackingService {

	private final FoodOrderRepository foodOrderRepository;
	private final DeliveryRepository deliveryRepository;
	private final AuthenticatedUserService authenticatedUserService;
	private final OwnershipAuthorizationService ownershipAuthorizationService;
	private final NotificationService notificationService;

	@Transactional(readOnly = true)
	public OrderTrackingResponse getTracking(Long orderId) {
		User actor = authenticatedUserService.getCurrentUser();
		FoodOrder order = findOrder(orderId);
		ownershipAuthorizationService.assertCanViewOrder(actor, order);

		Delivery delivery = deliveryRepository.findByOrderId(orderId).orElse(null);
		return toResponse(order, delivery);
	}

	@Transactional
	public OrderTrackingResponse updateTracking(Long orderId, OrderTrackingUpdateRequest request) {
		User actor = authenticatedUserService.getCurrentUser();
		FoodOrder order = findOrder(orderId);

		if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.DELIVERY_PERSON) {
			throw new AccessDeniedException("Only delivery partners/admin can update tracking");
		}

		Delivery delivery = deliveryRepository.findByOrderId(orderId)
				.orElseThrow(() -> new AccessDeniedException("Delivery assignment required"));
		if (actor.getRole() == Role.DELIVERY_PERSON) {
			ownershipAuthorizationService.verifyDeliveryAccess(actor, delivery);
		}
		else {
			ownershipAuthorizationService.verifyOrderAccess(actor, order);
		}

		Map<String, Object> tracking = new HashMap<>(delivery.getTrackingData() == null ? Map.of() : delivery.getTrackingData());
		if (request.getLatitude() != null) {
			tracking.put("latitude", request.getLatitude());
		}
		if (request.getLongitude() != null) {
			tracking.put("longitude", request.getLongitude());
		}
		if (request.getNote() != null && !request.getNote().isBlank()) {
			tracking.put("note", request.getNote().trim());
		}
		tracking.put("updatedAt", Instant.now().toString());

		delivery.setTrackingData(tracking);
		delivery.setDeliveryPerson(delivery.getDeliveryPerson() == null ? order.getDeliveryPerson() : delivery.getDeliveryPerson());
		delivery.setEtaAt(request.getEtaAt() == null ? delivery.getEtaAt() : request.getEtaAt());

		Delivery savedDelivery = deliveryRepository.save(delivery);
		notificationService.notifyOrderTrackingUpdate(order, savedDelivery.getTrackingData(), savedDelivery.getEtaAt());
		return toResponse(order, savedDelivery);
	}

	private FoodOrder findOrder(Long orderId) {
		return foodOrderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));
	}

	private OrderTrackingResponse toResponse(FoodOrder order, Delivery delivery) {
		return OrderTrackingResponse.builder()
				.orderId(order.getId())
				.deliveryPersonId(order.getDeliveryPerson() == null ? null : order.getDeliveryPerson().getId())
				.status(order.getStatus().name())
				.etaAt(delivery == null ? null : delivery.getEtaAt())
				.trackingData(delivery == null || delivery.getTrackingData() == null ? Map.of() : delivery.getTrackingData())
				.updatedAt(order.getUpdatedAt())
				.build();
	}
}
