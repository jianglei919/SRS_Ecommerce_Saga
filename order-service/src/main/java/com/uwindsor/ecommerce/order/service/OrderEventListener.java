package com.uwindsor.ecommerce.order.service;

import com.uwindsor.ecommerce.order.config.RabbitMQConfig;
import com.uwindsor.ecommerce.order.event.InventoryReservedEvent;
import com.uwindsor.ecommerce.order.event.InventoryReservationFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Order Event Listener - Listens to events from other services
 * Handles inventory reservation responses (both success and failure)
 */
@Slf4j
@Service
public class OrderEventListener {

    private final OrderService orderService;

    public OrderEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Listen for InventoryReservedEvent
     * Called when Inventory Service successfully reserves stock
     *
     * @param event Inventory reserved event
     */
    @RabbitListener(queues = RabbitMQConfig.INVENTORY_RESERVED_QUEUE)
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Received InventoryReservedEvent for order: {}, saga: {}", 
                event.getOrderId(), event.getSagaId());

        try {
            if (event.getSuccess()) {
                // Happy path - inventory successfully reserved
                orderService.handleInventoryReserved(event.getOrderId(), event.getSagaId());
            } else {
                // Should not happen with this event, but handle gracefully
                log.warn("Received success=false in InventoryReservedEvent, treating as failure");
                orderService.handleInventoryReservationFailed(
                        event.getOrderId(),
                        event.getSagaId(),
                        "Inventory reservation marked as failed"
                );
            }
        } catch (Exception e) {
            log.error("Error processing InventoryReservedEvent", e);
            // In production, might want to publish to a dead-letter queue
        }
    }

    /**
     * Listen for InventoryReservationFailedEvent
     * Called when Inventory Service fails to reserve stock
     *
     * @param event Inventory reservation failed event
     */
    @RabbitListener(queues = RabbitMQConfig.INVENTORY_FAILED_QUEUE)
    public void handleInventoryReservationFailed(InventoryReservationFailedEvent event) {
        log.warn("Received InventoryReservationFailedEvent for order: {}, saga: {}, reason: {}",
                event.getOrderId(), event.getSagaId(), event.getReason());

        try {
            // Trigger compensation - cancel the order
            orderService.handleInventoryReservationFailed(
                    event.getOrderId(),
                    event.getSagaId(),
                    event.getReason()
            );
        } catch (Exception e) {
            log.error("Error processing InventoryReservationFailedEvent", e);
            // In production, might want to publish to a dead-letter queue
        }
    }
}
