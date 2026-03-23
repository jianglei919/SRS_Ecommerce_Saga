package com.uwindsor.ecommerce.inventory.service;

import com.uwindsor.ecommerce.inventory.config.RabbitMQConfig;
import com.uwindsor.ecommerce.inventory.dto.OrderCreatedEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Inventory Event Listener - Listens for events from Order Service
 * Handles OrderCreatedEvent and triggers inventory reservation
 */
@Slf4j
@Service
public class InventoryEventListener {

    private final InventoryService inventoryService;

    public InventoryEventListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Listen for OrderCreatedEvent
     * Called when Order Service publishes a new order
     * Attempts to reserve inventory for the order
     *
     * @param event Order created event
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedEventDTO event) {
        log.info("Received OrderCreatedEvent for order: {}, saga: {}", 
                event.getOrderId(), event.getSagaId());

        try {
            // Attempt to reserve inventory
            inventoryService.reserveInventory(event);
        } catch (Exception e) {
            log.error("Error processing OrderCreatedEvent", e);
            // In production, might want to publish to a dead-letter queue
        }
    }
}
