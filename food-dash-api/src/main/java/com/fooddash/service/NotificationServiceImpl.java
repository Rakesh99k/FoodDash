package com.fooddash.service;

import com.fooddash.model.FoodOrder;
import com.fooddash.model.Notification;
import com.fooddash.model.Payment;
import com.fooddash.model.User;
import com.fooddash.repository.NotificationRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private final SimpMessagingTemplate messagingTemplate;
	private final NotificationRepository notificationRepository;

	@Override
	@Transactional
	public void notifyOrderPlaced(FoodOrder order) {
		sendPersistedNotification(
				order.getRestaurant().getOwner(),
				order,
				"ORDER_PLACED",
				"New order received",
				"Order #" + order.getId() + " has been placed.",
				payloadFor(order, "ORDER_PLACED"));
	}

	@Override
	@Transactional
	public void notifyPaymentCompleted(Payment payment) {
		FoodOrder order = payment.getOrder();
		Map<String, Object> payload = payloadFor(order, "PAYMENT_COMPLETED");
		payload.put("paymentId", payment.getId());
		payload.put("paymentStatus", payment.getStatus().name());
		payload.put("provider", payment.getProvider().name());
		sendPersistedNotification(
				order.getCustomer(),
				order,
				"PAYMENT_COMPLETED",
				"Payment completed",
				"Payment for order #" + order.getId() + " was completed.",
				payload);
		sendPersistedNotification(
				order.getRestaurant().getOwner(),
				order,
				"PAYMENT_COMPLETED",
				"Payment completed",
				"Payment for order #" + order.getId() + " was completed.",
				payload);
	}

	@Override
	@Transactional
	public void notifyOrderConfirmed(FoodOrder order) {
		sendPersistedNotification(
				order.getCustomer(),
				order,
				"ORDER_CONFIRMED",
				"Order confirmed",
				"Order #" + order.getId() + " has been confirmed.",
				payloadFor(order, "ORDER_CONFIRMED"));
	}

	@Override
	@Transactional
	public void notifyOutForDelivery(FoodOrder order) {
		Map<String, Object> payload = payloadFor(order, "OUT_FOR_DELIVERY");
		sendPersistedNotification(
				order.getCustomer(),
				order,
				"OUT_FOR_DELIVERY",
				"Out for delivery",
				"Order #" + order.getId() + " is out for delivery.",
				payload);
		if (order.getDeliveryPerson() != null) {
			sendPersistedNotification(
					order.getDeliveryPerson(),
					order,
					"OUT_FOR_DELIVERY",
					"Delivery assigned",
					"Order #" + order.getId() + " is out for delivery.",
					payload);
		}
	}

	@Override
	@Transactional
	public void notifyDelivered(FoodOrder order) {
		Map<String, Object> payload = payloadFor(order, "DELIVERED");
		sendPersistedNotification(
				order.getCustomer(),
				order,
				"DELIVERED",
				"Order delivered",
				"Order #" + order.getId() + " has been delivered.",
				payload);
		if (order.getDeliveryPerson() != null) {
			sendPersistedNotification(
					order.getDeliveryPerson(),
					order,
					"DELIVERED",
					"Delivery completed",
					"Order #" + order.getId() + " has been delivered.",
					payload);
		}
	}

	@Override
	public void notifyRestaurantOfNewOrder(FoodOrder order) {
		notifyOrderPlaced(order);
	}

	@Override
	public void notifyCustomerOfOrderStatusChange(FoodOrder order) {
		if (order.getStatus() == null) {
			return;
		}
		switch (order.getStatus()) {
			case CONFIRMED -> notifyOrderConfirmed(order);
			case OUT_FOR_DELIVERY -> notifyOutForDelivery(order);
			case DELIVERED -> notifyDelivered(order);
			default -> sendPersistedNotification(
					order.getCustomer(),
					order,
					"ORDER_STATUS_UPDATED",
					"Order status updated",
					"Order #" + order.getId() + " status changed to " + order.getStatus().name().toLowerCase(),
					payloadFor(order, "ORDER_STATUS_UPDATED"));
		}
	}

	@Override
	@Transactional
	public void notifyOrderTrackingUpdate(FoodOrder order, Map<String, Object> trackingData, Instant etaAt) {
		Map<String, Object> payload = payloadFor(order, "ORDER_TRACKING_UPDATED");
		payload.put("tracking", trackingData == null ? Map.of() : trackingData);
		payload.put("etaAt", etaAt);

		sendPersistedNotification(
				order.getCustomer(),
				order,
				"ORDER_TRACKING_UPDATED",
				"Order tracking updated",
				"Tracking for order #" + order.getId() + " has been updated.",
				payload);
		if (order.getDeliveryPerson() != null) {
			sendPersistedNotification(
					order.getDeliveryPerson(),
					order,
					"ORDER_TRACKING_UPDATED",
					"Order tracking updated",
					"Tracking for order #" + order.getId() + " has been updated.",
					payload);
		}
	}

	private void sendPersistedNotification(
			User recipient,
			FoodOrder order,
			String type,
			String title,
			String message,
			Map<String, Object> data) {
		if (recipient == null) {
			return;
		}

		Notification notification = notificationRepository.save(Notification.builder()
				.user(recipient)
				.order(order)
				.type(type)
				.channel("in_app")
				.status("sent")
				.title(title)
				.message(message)
				.data(data == null ? Map.of() : new HashMap<>(data))
				.sentAt(Instant.now())
				.build());

		Map<String, Object> payload = new HashMap<>(notification.getData());
		payload.put("notificationId", notification.getId());
		payload.put("type", notification.getType());
		payload.put("title", notification.getTitle());
		payload.put("message", notification.getMessage());
		payload.put("createdAt", notification.getCreatedAt());

		messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/notifications", payload);
		log.info("Notification {} delivered to user {}", notification.getType(), recipient.getId());
	}

	private Map<String, Object> payloadFor(FoodOrder order, String event) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("event", event);
		payload.put("orderId", order.getId());
		payload.put("status", order.getStatus() == null ? null : order.getStatus().name());
		payload.put("restaurantId", order.getRestaurant().getId());
		payload.put("customerId", order.getCustomer().getId());
		payload.put("updatedAt", order.getUpdatedAt());
		return payload;
	}
}
