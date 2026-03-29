package com.uwindsor.ecommerce.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inventory Entity - Represents product inventory
 * Tracks stock and reserved quantities
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "inventory")
public class Inventory {
    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Integer reserved;

    @Column(nullable = false)
    private Integer price;

    /**
     * Calculate available stock (total - reserved)
     */
    public Integer getAvailable() {
        return stock - reserved;
    }

    /**
     * Can reserve the given quantity?
     */
    public boolean canReserve(Integer quantity) {
        return getAvailable() >= quantity;
    }

    /**
     * Reserve quantity
     */
    public void reserve(Integer quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Insufficient stock. Available: " + getAvailable() + ", Requested: " + quantity);
        }
        this.reserved += quantity;
    }

    /**
     * Release/unreserve quantity
     */
    public void release(Integer quantity) {
        this.reserved -= quantity;
        if (this.reserved < 0) {
            this.reserved = 0;
        }
    }
}
