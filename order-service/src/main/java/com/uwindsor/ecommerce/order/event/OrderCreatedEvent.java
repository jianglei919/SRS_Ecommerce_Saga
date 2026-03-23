package com.uwindsor.ecommerce.order.event;

import com.uwindsor.ecommerce.order.dto.OrderItemDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * OrderCreatedEvent - Published when an order is created
 * This event triggers the inventory reservation saga step
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sagaId;
    private String orderId;
    private Long userId;
    private List<OrderItemDTO> items;
    private BigDecimal totalAmount;
}
