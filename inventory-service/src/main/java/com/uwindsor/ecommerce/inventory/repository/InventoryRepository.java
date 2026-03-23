package com.uwindsor.ecommerce.inventory.repository;

import com.uwindsor.ecommerce.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Inventory Repository - Data access layer for Inventory entity
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
}
