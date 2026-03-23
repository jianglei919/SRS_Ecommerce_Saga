package com.uwindsor.ecommerce.order.repository;

import com.uwindsor.ecommerce.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Order Repository - Data access layer for Order entity
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderId(String orderId);
    Optional<Order> findBySagaId(String sagaId);
}
