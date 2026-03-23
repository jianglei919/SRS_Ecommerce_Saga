package com.uwindsor.ecommerce.order.event;

import com.uwindsor.ecommerce.order.dto.OrderItemDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * InventoryReservedEvent - Published by Inventory Service after successful reservation
 * This event confirms the saga and completes the order
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sagaId;
    private String orderId;
    private Boolean success;
    private List<OrderItemDTO> reservedItems;
}
