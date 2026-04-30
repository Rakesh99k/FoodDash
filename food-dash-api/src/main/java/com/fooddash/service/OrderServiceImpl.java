package com.fooddash.service;

import com.fooddash.dto.OrderCreateItemRequest;
import com.fooddash.dto.OrderCreateRequest;
import com.fooddash.dto.OrderItemResponse;
import com.fooddash.dto.OrderResponse;
import com.fooddash.dto.OrderStatusUpdateRequest;
import com.fooddash.exception.ResourceNotFoundException;
import com.fooddash.model.FoodOrder;
import com.fooddash.model.MenuItem;
import com.fooddash.model.OrderItem;
import com.fooddash.model.OrderStatus;
import com.fooddash.model.Restaurant;
import com.fooddash.model.Role;
import com.fooddash.model.User;
import com.fooddash.repository.FoodOrderRepository;
import com.fooddash.repository.MenuItemRepository;
import com.fooddash.repository.OrderItemRepository;
import com.fooddash.repository.RestaurantRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final FoodOrderRepository foodOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public OrderResponse createOrder(User user, OrderCreateRequest request) {
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        FoodOrder order = new FoodOrder();
        order.setCustomer(user);
        order.setRestaurant(restaurant);
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setNotes(request.getNotes());
        order.setPlacedAt(Instant.now());

        // Process items
        for (OrderCreateItemRequest itemReq : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Menu item not found: " + itemReq.getMenuItemId()));

            if (!menuItem.getRestaurant().getId().equals(restaurant.getId())) {
                throw new IllegalArgumentException("Menu item " + itemReq.getMenuItemId()
                        + " does not belong to restaurant " + restaurant.getId());
            }

            BigDecimal lineTotal = menuItem.getPrice().multiply(new BigDecimal(itemReq.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setUnitPrice(menuItem.getPrice());
            orderItem.setLineTotal(lineTotal);
            orderItem.setSpecialInstructions(itemReq.getSpecialInstructions());
            orderItems.add(orderItem);
        }

        order.setSubtotalAmount(subtotal);
        order.setTaxAmount(subtotal.multiply(new BigDecimal("0.10"))); // 10% tax for simplicity
        order.setDeliveryFee(new BigDecimal("5.00")); // Flat delivery fee
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(order.getSubtotalAmount().add(order.getTaxAmount()).add(order.getDeliveryFee())
                .subtract(order.getDiscountAmount()));

        FoodOrder savedOrder = foodOrderRepository.save(order);
        List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);

        notificationService.notifyRestaurantOfNewOrder(savedOrder);

        return mapToResponse(savedOrder, savedItems);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(User user) {
        List<FoodOrder> orders = new ArrayList<>();
        if (user.getRole() == Role.CUSTOMER) {
            orders = foodOrderRepository.findByCustomerIdOrderByCreatedAtDesc(user.getId());
        } else if (user.getRole() == Role.RESTAURANT_OWNER) {
            // Need to get all restaurants owned by user and their orders
            List<Restaurant> restaurants = restaurantRepository.findByOwnerId(user.getId());
            for (Restaurant r : restaurants) {
                orders.addAll(foodOrderRepository.findByRestaurantIdOrderByCreatedAtDesc(r.getId()));
            }
            // Sort merged list by placedAt
            orders.sort((o1, o2) -> o2.getPlacedAt().compareTo(o1.getPlacedAt()));
        } else if (user.getRole() == Role.DELIVERY_PERSON) {
            orders = foodOrderRepository.findByDeliveryPersonIdOrderByCreatedAtDesc(user.getId());
        }

        return orders.stream()
                .map(this::mapToResponseQuick)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetails(Long orderId, User user) {
        FoodOrder order = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        checkAuthorization(order, user);
        return mapToResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request, User user) {
        FoodOrder order = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        checkAuthorization(order, user);

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();

        // Business Rules Validate
        if (user.getRole() == Role.CUSTOMER) {
            if (newStatus != OrderStatus.CANCELLED) {
                throw new AuthorizationDeniedException("Customers can only update status to canceled");
            }
            if (currentStatus != OrderStatus.PENDING && currentStatus != OrderStatus.CONFIRMED) {
                throw new IllegalArgumentException("Cannot cancel order in status: " + currentStatus);
            }
        } else if (user.getRole() == Role.RESTAURANT_OWNER) {
            if (newStatus != OrderStatus.CONFIRMED && newStatus != OrderStatus.PREPARING
                    && newStatus != OrderStatus.READY_FOR_PICKUP && newStatus != OrderStatus.CANCELLED) {
                throw new AuthorizationDeniedException("Invalid status update for restaurant owner");
            }
        } else if (user.getRole() == Role.DELIVERY_PERSON) {
            if (newStatus != OrderStatus.OUT_FOR_DELIVERY && newStatus != OrderStatus.DELIVERED) {
                throw new AuthorizationDeniedException("Invalid status update for delivery person");
            }
            if (order.getDeliveryPerson() == null) {
                // For simplicity, first delivery person to update it assigs themselves to it if
                // it was READY_FOR_PICKUP
                order.setDeliveryPerson(user);
            } else if (!order.getDeliveryPerson().getId().equals(user.getId())) {
                throw new AuthorizationDeniedException("Order is assigned to another delivery person");
            }
        }

        order.setStatus(newStatus);
        FoodOrder savedOrder = foodOrderRepository.save(order);

        notificationService.notifyCustomerOfOrderStatusChange(savedOrder);

        return mapToResponse(savedOrder, orderItemRepository.findByOrderId(orderId));
    }

    private void checkAuthorization(FoodOrder order, User user) {
        if (user.getRole() == Role.ADMIN)
            return;

        if (user.getRole() == Role.CUSTOMER && !order.getCustomer().getId().equals(user.getId())) {
            throw new AuthorizationDeniedException("Not authorized to view this order");
        }
        if (user.getRole() == Role.RESTAURANT_OWNER && !order.getRestaurant().getOwner().getId().equals(user.getId())) {
            throw new AuthorizationDeniedException("Not authorized to view this order");
        }
        if (user.getRole() == Role.DELIVERY_PERSON) {
            if (order.getDeliveryPerson() != null && !order.getDeliveryPerson().getId().equals(user.getId())) {
                throw new AuthorizationDeniedException("Order assigned to another delivery person");
            }
        }
    }

    private OrderResponse mapToResponseQuick(FoodOrder order) {
        return mapToResponse(order, orderItemRepository.findByOrderId(order.getId()));
    }

    private OrderResponse mapToResponse(FoodOrder order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream().map(i -> OrderItemResponse.builder()
                .id(i.getId())
                .menuItemId(i.getMenuItem().getId())
                .menuItemName(i.getMenuItem().getName())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .lineTotal(i.getLineTotal())
                .specialInstructions(i.getSpecialInstructions())
                .build()).collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .restaurantId(order.getRestaurant().getId())
                .restaurantName(order.getRestaurant().getName())
                .status(order.getStatus())
                .subtotalAmount(order.getSubtotalAmount())
                .taxAmount(order.getTaxAmount())
                .deliveryFee(order.getDeliveryFee())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .notes(order.getNotes())
                .deliveryAddress(order.getDeliveryAddress())
                .placedAt(order.getPlacedAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .build();
    }
}
