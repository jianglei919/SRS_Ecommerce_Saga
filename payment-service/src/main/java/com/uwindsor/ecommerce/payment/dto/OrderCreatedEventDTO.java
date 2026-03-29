package com.uwindsor.ecommerce.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * OrderCreatedEventDTO - Received from Order Service
 * Represents an order that was created and requires payment reservation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEventDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sagaId;
    private String orderId;
    private Long userId;
    private List<?> items;  // List of order items with productId and quantity
    private BigDecimal totalAmount;
}
