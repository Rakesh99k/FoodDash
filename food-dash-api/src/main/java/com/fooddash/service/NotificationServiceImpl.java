package com.fooddash.service;

import com.fooddash.model.FoodOrder;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void notifyRestaurantOfNewOrder(FoodOrder order) {
        Map<String, Object> payload = basePayload(order);
        payload.put("event", "NEW_ORDER");

        messagingTemplate.convertAndSend(
                "/topic/restaurants/" + order.getRestaurant().getId() + "/orders", (Object) payload);

        log.info("Sending notification to restaurant {}: New order {} received.",
                order.getRestaurant().getId(), order.getId());
    }

    @Override
    public void notifyCustomerOfOrderStatusChange(FoodOrder order) {
        Map<String, Object> payload = basePayload(order);
        payload.put("event", "ORDER_STATUS_UPDATED");

        messagingTemplate.convertAndSend("/topic/orders/" + order.getId() + "/status", (Object) payload);
        messagingTemplate.convertAndSendToUser(
                order.getCustomer().getEmail(), "/queue/orders/status", payload);

        log.info("Sending notification to customer {}: Order {} status changed to {}.",
                order.getCustomer().getId(), order.getId(), order.getStatus());
    }

    @Override
    public void notifyOrderTrackingUpdate(FoodOrder order, Map<String, Object> trackingData, Instant etaAt) {
        Map<String, Object> payload = basePayload(order);
        payload.put("event", "ORDER_TRACKING_UPDATED");
        payload.put("tracking", trackingData == null ? Map.of() : trackingData);
        payload.put("etaAt", etaAt);

        messagingTemplate.convertAndSend("/topic/orders/" + order.getId() + "/tracking", (Object) payload);
        messagingTemplate.convertAndSendToUser(
                order.getCustomer().getEmail(), "/queue/orders/tracking", payload);
        if (order.getDeliveryPerson() != null) {
            messagingTemplate.convertAndSendToUser(
                    order.getDeliveryPerson().getEmail(), "/queue/orders/tracking", payload);
        }
    }

    private Map<String, Object> basePayload(FoodOrder order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId());
        payload.put("status", order.getStatus().name());
        payload.put("restaurantId", order.getRestaurant().getId());
        payload.put("customerId", order.getCustomer().getId());
        payload.put("updatedAt", order.getUpdatedAt());
        return payload;
    }
}
