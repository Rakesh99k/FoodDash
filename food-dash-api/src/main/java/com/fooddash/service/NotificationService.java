package com.fooddash.service;

import com.fooddash.model.FoodOrder;
import com.fooddash.model.Payment;
import java.time.Instant;
import java.util.Map;

public interface NotificationService {
    void notifyOrderPlaced(FoodOrder order);

    void notifyPaymentCompleted(Payment payment);

    void notifyOrderConfirmed(FoodOrder order);

    void notifyOutForDelivery(FoodOrder order);

    void notifyDelivered(FoodOrder order);

    void notifyRestaurantOfNewOrder(FoodOrder order);

    void notifyCustomerOfOrderStatusChange(FoodOrder order);

    void notifyOrderTrackingUpdate(FoodOrder order, Map<String, Object> trackingData, Instant etaAt);
}
