package com.uwindsor.ecommerce.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wallet")
public class Wallet {

    @Id
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance;

    @PrePersist
    public void onCreate() {
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
    }
}
