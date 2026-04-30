package com.fooddash.dto;

import com.fooddash.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long customerId;
    private Long restaurantId;
    private String restaurantName;
    private OrderStatus status;
    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal deliveryFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String notes;
    private Map<String, Object> deliveryAddress;
    private Instant placedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OrderItemResponse> items;
}
