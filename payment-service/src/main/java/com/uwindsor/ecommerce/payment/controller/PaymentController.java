package com.uwindsor.ecommerce.payment.controller;

import com.uwindsor.ecommerce.payment.entity.Payment;
import com.uwindsor.ecommerce.payment.repository.PaymentRepository;
import com.uwindsor.ecommerce.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public PaymentController(PaymentRepository paymentRepository, PaymentService paymentService) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    @PostMapping("/{orderId}")
    public Payment processPayment(@PathVariable String orderId, @RequestParam BigDecimal amount, @RequestParam String status) {
        Payment payment = Payment.builder()
            .orderId(orderId)
            .amount(amount)
            .status(status)
            .paymentTime(LocalDateTime.now())
            .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Let the service handle publishing the event back to order service
        // via RabbitMQ, so the saga can finalize or compensate
        paymentService.processPaymentResult(orderId, status);

        return savedPayment;
    }
}

