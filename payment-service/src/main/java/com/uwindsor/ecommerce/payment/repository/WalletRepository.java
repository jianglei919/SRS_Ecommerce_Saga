package com.uwindsor.ecommerce.payment.repository;

import com.uwindsor.ecommerce.payment.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
}
