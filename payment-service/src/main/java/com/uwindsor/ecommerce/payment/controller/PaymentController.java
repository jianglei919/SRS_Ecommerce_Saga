package com.uwindsor.ecommerce.payment.controller;

import com.uwindsor.ecommerce.payment.entity.Payment;
import com.uwindsor.ecommerce.payment.entity.Wallet;
import com.uwindsor.ecommerce.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{orderId}")
    public ResponseEntity<?> processPayment(@PathVariable String orderId) {
        try {
            Payment payment = paymentService.processPayment(orderId);
            return ResponseEntity.ok(payment);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            log.warn("Payment rejected for order {}: {}", orderId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Payment processing error for order {}", orderId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Payment processing failed"));
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Payment>> getRecentPayments() {
        return ResponseEntity.ok(paymentService.getRecentPayments());
    }

    @GetMapping("/wallet/{userId}")
    public ResponseEntity<Wallet> getWallet(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getWallet(userId));
    }

    @GetMapping("/processed-order-ids")
    public ResponseEntity<Set<String>> getProcessedOrderIds() {
        return ResponseEntity.ok(paymentService.getProcessedOrderIds());
    }

    @PutMapping("/wallet/{userId}")
    public ResponseEntity<?> setWalletBalance(@PathVariable Long userId, @RequestParam BigDecimal balance) {
        try {
            Wallet wallet = paymentService.setWalletBalance(userId, balance);
            return ResponseEntity.ok(wallet);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/test-data")
    public ResponseEntity<?> clearTestData() {
        try {
            paymentService.clearTestData();
            return ResponseEntity.ok(Map.of("message", "Payment and wallet test data cleared"));
        } catch (Exception ex) {
            log.error("Failed to clear payment test data", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to clear payment test data"));
        }
    }
}
