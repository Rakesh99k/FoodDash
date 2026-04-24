package com.fooddash.controller;

import com.fooddash.dto.ApiResponse;
import com.fooddash.dto.OrderCreateRequest;
import com.fooddash.dto.OrderResponse;
import com.fooddash.dto.OrderStatusUpdateRequest;
import com.fooddash.model.User;
import com.fooddash.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.createOrder(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(@AuthenticationPrincipal User user) {
        List<OrderResponse> responses = orderService.getOrdersForUser(user);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        OrderResponse response = orderService.getOrderDetails(id, user);
        return ResponseEntity.ok(ApiResponse.success("Order details retrieved", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        OrderResponse response = orderService.updateOrderStatus(id, request, user);
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", response));
    }
}
