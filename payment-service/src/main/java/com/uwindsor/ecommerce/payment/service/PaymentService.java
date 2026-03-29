package com.uwindsor.ecommerce.payment.service;

import com.uwindsor.ecommerce.payment.config.RabbitMQConfig;
import com.uwindsor.ecommerce.payment.dto.OrderSnapshotDTO;
import com.uwindsor.ecommerce.payment.entity.Payment;
import com.uwindsor.ecommerce.payment.entity.Wallet;
import com.uwindsor.ecommerce.payment.event.PaymentReservedEvent;
import com.uwindsor.ecommerce.payment.event.PaymentReservationFailedEvent;
import com.uwindsor.ecommerce.payment.repository.PaymentRepository;
import com.uwindsor.ecommerce.payment.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;
    private final String orderServiceBaseUrl;

    public PaymentService(PaymentRepository paymentRepository,
            WalletRepository walletRepository,
            RabbitTemplate rabbitTemplate,
            @Value("${app.order-service-base-url:http://localhost:8080}") String orderServiceBaseUrl) {
        this.paymentRepository = paymentRepository;
        this.walletRepository = walletRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.orderServiceBaseUrl = orderServiceBaseUrl;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public Payment processPayment(String orderId) {
        log.info("Processing payment request for order: {}", orderId);

        if (paymentRepository.existsByOrderId(orderId)) {
            throw new IllegalStateException("Payment already processed for order: " + orderId);
        }

        OrderSnapshotDTO order = fetchOrder(orderId);
        if (order == null) {
            throw new IllegalStateException("Order not found: " + orderId);
        }

        if (!"CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Order is not payable. Current status: " + order.getStatus());
        }

        Long userId = Objects.requireNonNull(order.getUserId(), "Order userId is required");

        Wallet wallet = walletRepository.findById(userId)
                .orElseGet(() -> walletRepository.save(
                        Wallet.builder().userId(userId).balance(BigDecimal.ZERO).build()));

        BigDecimal amount = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        boolean success = wallet.getBalance().compareTo(amount) >= 0;

        if (success) {
            wallet.setBalance(wallet.getBalance().subtract(amount));
            walletRepository.save(wallet);
        }

        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .status(success ? "SUCCESS" : "FAILED")
                .paymentTime(LocalDateTime.now())
                .build();
        Payment savedPayment = Objects.requireNonNull(paymentRepository.save(payment));

        if (success) {
            publishPaymentSuccess(orderId);
        } else {
            publishPaymentFailure(orderId, "Insufficient wallet balance");
        }

        return savedPayment;
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(Long userId) {
        return walletRepository.findById(Objects.requireNonNull(userId))
                .orElseGet(() -> Wallet.builder().userId(userId).balance(BigDecimal.ZERO).build());
    }

    @Transactional(readOnly = true)
    public Set<String> getProcessedOrderIds() {
        return new HashSet<>(paymentRepository.findAllOrderIds());
    }

    @Transactional(readOnly = true)
    public List<Payment> getRecentPayments() {
        return paymentRepository.findTop20ByOrderByPaymentTimeDesc();
    }

    @Transactional
    public Wallet setWalletBalance(Long userId, BigDecimal balance) {
        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance must be >= 0");
        }

        Long nonNullUserId = Objects.requireNonNull(userId, "userId is required");

        Wallet wallet = walletRepository.findById(nonNullUserId)
                .orElseGet(() -> Wallet.builder().userId(userId).balance(BigDecimal.ZERO).build());
        wallet.setBalance(balance);
        return walletRepository.save(wallet);
    }

    @Transactional
    public void clearTestData() {
        log.warn("Clearing payment and wallet test data");
        paymentRepository.deleteAllInBatch();
        walletRepository.deleteAllInBatch();
    }

    private OrderSnapshotDTO fetchOrder(String orderId) {
        String url = orderServiceBaseUrl + "/api/orders/" + orderId;
        try {
            return restTemplate.getForObject(url, OrderSnapshotDTO.class);
        } catch (RestClientException ex) {
            log.error("Failed to fetch order {} from order-service", orderId, ex);
            throw new IllegalStateException("Cannot validate order status for payment");
        }
    }

    private void publishPaymentSuccess(String orderId) {
        PaymentReservedEvent successEvent = PaymentReservedEvent.builder()
                .sagaId("unknown-from-payment")
                .orderId(orderId)
                .success(true)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_RESERVED_ROUTING_KEY,
                successEvent);
        log.info("Payment success event published for order: {}", orderId);
    }

    private void publishPaymentFailure(String orderId, String reason) {
        PaymentReservationFailedEvent failureEvent = PaymentReservationFailedEvent.builder()
                .sagaId("unknown-from-payment")
                .orderId(orderId)
                .success(false)
                .reason(reason)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_FAILED_ROUTING_KEY,
                failureEvent);
        log.warn("Payment failure event published for order: {}, reason: {}", orderId, reason);
    }
}
