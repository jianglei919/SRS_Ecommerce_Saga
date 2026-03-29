package com.uwindsor.ecommerce.payment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.persistence.Column(nullable = false, unique = true)
    private String orderId;

    @jakarta.persistence.Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @jakarta.persistence.Column(nullable = false)
    private String status;

    @jakarta.persistence.Column(nullable = false)
    private LocalDateTime paymentTime;
}
