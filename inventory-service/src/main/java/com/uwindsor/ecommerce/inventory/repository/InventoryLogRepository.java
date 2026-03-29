package com.uwindsor.ecommerce.inventory.repository;

import com.uwindsor.ecommerce.inventory.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * InventoryLog Repository - Data access layer for InventoryLog entity
 */
@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    List<InventoryLog> findByOrderId(String orderId);

    java.util.Optional<InventoryLog> findTopByProductIdOrderByTimestampDesc(Long productId);
}
