package com.fooddash.service;

import com.fooddash.model.FoodOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void notifyRestaurantOfNewOrder(FoodOrder order) {
        // Implement WebSocket/Push notification logic here
        log.info("Sending notification to restaurant {}: New order {} received.",
                order.getRestaurant().getId(), order.getId());
    }

    @Override
    public void notifyCustomerOfOrderStatusChange(FoodOrder order) {
        // Implement WebSocket/Push notification logic here
        log.info("Sending notification to customer {}: Order {} status changed to {}.",
                order.getCustomer().getId(), order.getId(), order.getStatus());
    }
}
