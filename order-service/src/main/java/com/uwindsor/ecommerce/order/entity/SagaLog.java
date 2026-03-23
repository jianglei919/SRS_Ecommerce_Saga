package com.uwindsor.ecommerce.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * SagaLog Entity - Logs the saga progression and each step status
 * Used to track saga state and support compensation if needed
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "saga_log")
public class SagaLog {
    @Id
    @Column(name = "saga_id")
    private String sagaId;

    @Column(name = "current_step")
    private String currentStep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    /**
     * Saga status enum
     */
    public enum SagaStatus {
        STARTED,       // Saga started
        COMPLETED,     // Saga completed successfully
        COMPENSATED,   // Saga compensated due to failure
        FAILED         // Saga failed permanently
    }
}
