package com.fooddash.service;

import com.fooddash.dto.OrderCreateRequest;
import com.fooddash.dto.OrderResponse;
import com.fooddash.dto.OrderStatusUpdateRequest;
import com.fooddash.model.User;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(User user, OrderCreateRequest request);

    List<OrderResponse> getOrdersForUser(User user);

    OrderResponse getOrderDetails(Long orderId, User user);

    OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request, User user);
}
