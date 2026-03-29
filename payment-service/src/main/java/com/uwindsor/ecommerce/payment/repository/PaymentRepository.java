package com.uwindsor.ecommerce.payment.repository;
import com.uwindsor.ecommerce.payment.entity.Payment;
import org.springframework.data.repository.CrudRepository;
public interface PaymentRepository extends CrudRepository<Payment, Long> {}
