package com.uwindsor.ecommerce.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order Entity - Represents an order in the system
 * Status: PENDING -> CONFIRMED (success) or CANCELLED (failure)
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "product_names")
    private String productNames;

    private String sagaId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }

    /**
     * Order status enum
     */
    public enum OrderStatus {
        PENDING,      // Order created, waiting for inventory confirmation
        CONFIRMED,    // Order confirmed, inventory reserved
        CANCELLED,    // Order cancelled due to failure or user action
        FAILED        // Order processing failed permanently
    }
}
