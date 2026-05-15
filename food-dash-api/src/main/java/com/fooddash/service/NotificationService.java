package com.fooddash.service;

import com.fooddash.model.FoodOrder;
import java.time.Instant;
import java.util.Map;

public interface NotificationService {
    void notifyRestaurantOfNewOrder(FoodOrder order);

    void notifyCustomerOfOrderStatusChange(FoodOrder order);

    void notifyOrderTrackingUpdate(FoodOrder order, Map<String, Object> trackingData, Instant etaAt);
}
