package com.uwindsor.ecommerce.inventory.service;

import com.uwindsor.ecommerce.inventory.config.RabbitMQConfig;
import com.uwindsor.ecommerce.inventory.dto.OrderCreatedEventDTO;
import com.uwindsor.ecommerce.inventory.entity.Inventory;
import com.uwindsor.ecommerce.inventory.entity.InventoryLog;
import com.uwindsor.ecommerce.inventory.event.InventoryReservedEvent;
import com.uwindsor.ecommerce.inventory.event.InventoryReservationFailedEvent;
import com.uwindsor.ecommerce.inventory.repository.InventoryLogRepository;
import com.uwindsor.ecommerce.inventory.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inventory Service - Handles inventory reservation and compensation
 * This service listens for OrderCreatedEvent and attempts to reserve inventory
 * Publishes InventoryReservedEvent or InventoryReservationFailedEvent
 */
@Slf4j
@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final RabbitTemplate rabbitTemplate;

    public InventoryService(InventoryRepository inventoryRepository,
                           InventoryLogRepository inventoryLogRepository,
                           RabbitTemplate rabbitTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryLogRepository = inventoryLogRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Reserve inventory for an order
     * This is the saga step that responds to OrderCreatedEvent
     *
     * @param event Order created event with order details and items
     */
    @Transactional
    public void reserveInventory(OrderCreatedEventDTO event) {
        log.info("Processing inventory reservation for order: {}, saga: {}", 
                event.getOrderId(), event.getSagaId());

        try {
            // Check and reserve each item in the order
            for (Object itemObj : event.getItems()) {
                // Parse item from Map/DTO
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> item = (java.util.Map<String, Object>) itemObj;
                Long productId = Long.parseLong(item.get("productId").toString());
                Integer quantity = Integer.parseInt(item.get("quantity").toString());

                // Fetch inventory
                Inventory inventory = inventoryRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

                // Check if enough stock is available
                if (inventory.getAvailable() < quantity) {
                    // Insufficient stock - publish failure event
                    publishReservationFailure(event, "Insufficient stock for product: " + productId + 
                            ". Available: " + inventory.getAvailable() + ", Requested: " + quantity);
                    return;
                }

                // Reserve inventory (local ACID transaction)
                inventory.reserve(quantity);
                inventoryRepository.save(inventory);

                // Log the reservation
                InventoryLog log = InventoryLog.builder()
                        .orderId(event.getOrderId())
                        .productId(productId)
                        .quantity(quantity)
                        .action(InventoryLog.InventoryAction.RESERVE)
                        .build();
                inventoryLogRepository.save(log);

                log.info("Inventory reserved for product: {}, quantity: {}, order: {}", 
                        productId, quantity, event.getOrderId());
            }

            // All items successfully reserved
            publishReservationSuccess(event);

        } catch (Exception e) {
            log.error("Error during inventory reservation for order: " + event.getOrderId(), e);
            publishReservationFailure(event, "Error during reservation: " + e.getMessage());
        }
    }

    /**
     * Release/compensate inventory for a failed order
     * Called when order fails or compensation is needed
     *
     * @param orderId Order ID
     */
    @Transactional
    public void releaseInventory(String orderId) {
        log.info("Releasing inventory for order: {}", orderId);

        try {
            // Find all reservations for this order
            java.util.List<InventoryLog> logs = inventoryLogRepository.findByOrderId(orderId);

            for (InventoryLog log : logs) {
                if (log.getAction() == InventoryLog.InventoryAction.RESERVE) {
                    // Release the reservation
                    Inventory inventory = inventoryRepository.findById(log.getProductId())
                            .orElseThrow(() -> new RuntimeException("Product not found: " + log.getProductId()));

                    inventory.release(log.getQuantity());
                    inventoryRepository.save(inventory);

                    // Log the release
                    InventoryLog releaseLog = InventoryLog.builder()
                            .orderId(orderId)
                            .productId(log.getProductId())
                            .quantity(log.getQuantity())
                            .action(InventoryLog.InventoryAction.RELEASE)
                            .build();
                    inventoryLogRepository.save(releaseLog);

                    log.info("Inventory released for product: {}, quantity: {}, order: {}", 
                            log.getProductId(), log.getQuantity(), orderId);
                }
            }
        } catch (Exception e) {
            log.error("Error releasing inventory for order: " + orderId, e);
        }
    }

    /**
     * Publish successful inventory reservation event
     */
    private void publishReservationSuccess(OrderCreatedEventDTO event) {
        InventoryReservedEvent successEvent = InventoryReservedEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(true)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.INVENTORY_RESERVED_ROUTING_KEY,
                successEvent
        );
        log.info("InventoryReservedEvent (success) published for order: {}, saga: {}", 
                event.getOrderId(), event.getSagaId());
    }

    /**
     * Publish reservation failure event
     */
    private void publishReservationFailure(OrderCreatedEventDTO event, String reason) {
        InventoryReservationFailedEvent failureEvent = InventoryReservationFailedEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(false)
                .reason(reason)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.INVENTORY_FAILED_ROUTING_KEY,
                failureEvent
        );
        log.warn("InventoryReservationFailedEvent published for order: {}, saga: {}, reason: {}", 
                event.getOrderId(), event.getSagaId(), reason);
    }
}
