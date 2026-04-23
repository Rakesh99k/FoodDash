package com.fooddash.service;

import com.fooddash.model.FoodOrder;

public interface NotificationService {
    void notifyRestaurantOfNewOrder(FoodOrder order);

    void notifyCustomerOfOrderStatusChange(FoodOrder order);
}
