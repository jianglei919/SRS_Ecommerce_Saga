package com.uwindsor.ecommerce.order.controller;

import com.uwindsor.ecommerce.order.dto.CreateOrderRequest;
import com.uwindsor.ecommerce.order.dto.CreateOrderResponse;
import com.uwindsor.ecommerce.order.entity.Order;
import com.uwindsor.ecommerce.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Order Controller - REST API endpoints for order operations
 * Provides endpoints for creating orders and checking order status
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * POST /api/orders - Create a new order
     * Initiates the saga by creating an order and publishing OrderCreatedEvent
     *
     * @param request Create order request
     * @return Response with order ID and status
     */
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Received order creation request for user: {}", request.getUserId());
        try {
            CreateOrderResponse response = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/orders/{orderId} - Get order details and status
     *
     * @param orderId Order ID
     * @return Order details
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        log.info("Fetching order: {}", orderId);
        try {
            Order order = orderService.getOrderByOrderId(orderId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Order not found: {}", orderId);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/test-data")
    public ResponseEntity<?> clearTestData() {
        try {
            orderService.clearTestData();
            return ResponseEntity.ok(Map.of("message", "Order and saga test data cleared"));
        } catch (Exception e) {
            log.error("Failed to clear order test data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to clear order test data"));
        }
    }
}
