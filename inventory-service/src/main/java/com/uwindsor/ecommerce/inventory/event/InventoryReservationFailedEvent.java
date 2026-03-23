package com.uwindsor.ecommerce.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * InventoryReservationFailedEvent - Published on inventory reservation failure
 * Sent back to Order Service to trigger compensation
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
