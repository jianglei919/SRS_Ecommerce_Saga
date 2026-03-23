package com.uwindsor.ecommerce.order.repository;

import com.uwindsor.ecommerce.order.entity.SagaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * SagaLog Repository - Data access layer for SagaLog entity
 */
@Repository
public interface SagaLogRepository extends JpaRepository<SagaLog, String> {
}
