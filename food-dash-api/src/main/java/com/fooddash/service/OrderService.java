package com.fooddash.service;

import com.fooddash.dto.OrderCreateRequest;
import com.fooddash.dto.OrderResponse;
import com.fooddash.dto.OrderStatusUpdateRequest;
import java.util.List;

public interface OrderService {

    OrderResponse createOrder(OrderCreateRequest request);

    List<OrderResponse> getOrdersForUser();

    OrderResponse getOrderDetails(Long orderId);

    OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request);
}
