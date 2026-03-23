package com.uwindsor.ecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * InventoryReservationFailedEvent - Published by Inventory Service on reservation failure
 * This event triggers compensation logic (cancel order)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservationFailedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sagaId;
    private String orderId;
    private Boolean success = false;
    private String reason;
}
