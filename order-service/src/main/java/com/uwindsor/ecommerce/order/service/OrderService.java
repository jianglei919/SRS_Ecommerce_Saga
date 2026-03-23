package com.uwindsor.ecommerce.order.service;

import com.uwindsor.ecommerce.order.config.RabbitMQConfig;
import com.uwindsor.ecommerce.order.dto.CreateOrderRequest;
import com.uwindsor.ecommerce.order.dto.CreateOrderResponse;
import com.uwindsor.ecommerce.order.entity.Order;
import com.uwindsor.ecommerce.order.entity.SagaLog;
import com.uwindsor.ecommerce.order.event.OrderCreatedEvent;
import com.uwindsor.ecommerce.order.repository.OrderRepository;
import com.uwindsor.ecommerce.order.repository.SagaLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Order Service - Core business logic for order operations and saga orchestration
 * This service acts as the saga orchestrator, managing the distributed transaction
 */
@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final SagaLogRepository sagaLogRepository;
    private final RabbitTemplate rabbitTemplate;

    public OrderService(OrderRepository orderRepository,
                       SagaLogRepository sagaLogRepository,
                       RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.sagaLogRepository = sagaLogRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Create order and initiate saga
     * This is the happy path where order is created with PENDING status
     * and OrderCreatedEvent is published to trigger inventory service
     *
     * @param request Create order request containing user ID and items
     * @return Response with order ID and status
     */
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        // Generate unique identifiers
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sagaId = UUID.randomUUID().toString();

        // Calculate total amount (simplified - assumes pre-calculation from client)
        BigDecimal totalAmount = calculateTotalAmount(request);

        // Step 1: Create order locally with PENDING status (local ACID transaction)
        Order order = Order.builder()
                .orderId(orderId)
                .userId(request.getUserId())
                .totalAmount(totalAmount)
                .status(Order.OrderStatus.PENDING)
                .sagaId(sagaId)
                .build();
        orderRepository.save(order);
        log.info("Order created with ID: {}, Saga ID: {}, Status: PENDING", orderId, sagaId);

        // Step 2: Create saga log entry
        SagaLog sagaLog = SagaLog.builder()
                .sagaId(sagaId)
                .currentStep("ORDER_CREATED")
                .status(SagaLog.SagaStatus.STARTED)
                .build();
        sagaLogRepository.save(sagaLog);
        log.info("Saga log created for saga ID: {}", sagaId);

        // Step 3: Publish OrderCreatedEvent to RabbitMQ
        // This triggers the Inventory Service to attempt reservation
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .userId(request.getUserId())
                .items(request.getItems())
                .totalAmount(totalAmount)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                event
        );
        log.info("OrderCreatedEvent published to RabbitMQ for saga ID: {}", sagaId);

        return CreateOrderResponse.builder()
                .orderId(orderId)
                .status("PENDING")
                .build();
    }

    /**
     * Handle inventory reservation success
     * Called when Inventory Service successfully reserves stock
     * Updates order status to CONFIRMED and completes saga
     *
     * @param orderId Order ID
     * @param sagaId Saga ID
     */
    @Transactional
    public void handleInventoryReserved(String orderId, String sagaId) {
        log.info("Handling inventory reserved for order: {}, saga: {}", orderId, sagaId);

        // Fetch and update order
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setStatus(Order.OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("Order confirmed: {}", orderId);

        // Update saga log
        SagaLog sagaLog = sagaLogRepository.findById(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga log not found: " + sagaId));
        sagaLog.setCurrentStep("SAGA_COMPLETED");
        sagaLog.setStatus(SagaLog.SagaStatus.COMPLETED);
        sagaLogRepository.save(sagaLog);
        log.info("Saga completed successfully: {}", sagaId);
    }

    /**
     * Handle inventory reservation failure
     * Called when Inventory Service fails to reserve stock
     * Triggers compensation: order is cancelled
     *
     * @param orderId Order ID
     * @param sagaId Saga ID
     * @param reason Reason for failure
     */
    @Transactional
    public void handleInventoryReservationFailed(String orderId, String sagaId, String reason) {
        log.error("Handling inventory reservation failure for order: {}, reason: {}", orderId, reason);

        // Fetch and update order - compensation
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order cancelled due to inventory failure: {}", orderId);

        // Update saga log
        SagaLog sagaLog = sagaLogRepository.findById(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga log not found: " + sagaId));
        sagaLog.setCurrentStep("COMPENSATED");
        sagaLog.setStatus(SagaLog.SagaStatus.COMPENSATED);
        sagaLogRepository.save(sagaLog);
        log.info("Saga compensated: {}", sagaId);
    }

    /**
     * Get order by order ID
     */
    public Order getOrderByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    /**
     * Calculate total amount (simplified - assuming items already have prices)
     * In production, this would fetch prices from inventory service
     */
    private BigDecimal calculateTotalAmount(CreateOrderRequest request) {
        // Simplified calculation - in production, fetch actual prices
        // For now, assume each item quantity is multiplied by a default price
        BigDecimal total = BigDecimal.ZERO;
        // This is a placeholder - actual implementation would get prices from product catalog
        return BigDecimal.valueOf(1000); // Default for demo
    }
}
