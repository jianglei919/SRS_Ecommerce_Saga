package com.uwindsor.ecommerce.payment.repository;

import com.uwindsor.ecommerce.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByOrderId(String orderId);

    @Query("select p.orderId from Payment p")
    List<String> findAllOrderIds();

    List<Payment> findTop20ByOrderByPaymentTimeDesc();
}
