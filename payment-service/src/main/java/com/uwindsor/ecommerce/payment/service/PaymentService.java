package com.uwindsor.ecommerce.payment.service;

import com.uwindsor.ecommerce.payment.config.RabbitMQConfig;
import com.uwindsor.ecommerce.payment.event.PaymentReservedEvent;
import com.uwindsor.ecommerce.payment.event.PaymentReservationFailedEvent;
import com.uwindsor.ecommerce.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;

    public PaymentService(PaymentRepository paymentRepository,
                           RabbitTemplate rabbitTemplate) {
        this.paymentRepository = paymentRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void processPaymentResult(String orderId, String status) {
        log.info("Processing payment result from frontend for order: {}, status: {}", orderId, status);

        if ("SUCCESS".equalsIgnoreCase(status)) {
            PaymentReservedEvent successEvent = PaymentReservedEvent.builder()
                    .sagaId("unknown-from-payment") // order-service should use orderId
                    .orderId(orderId)
                    .success(true)
                    .build();
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EVENT_EXCHANGE,
                    RabbitMQConfig.PAYMENT_RESERVED_ROUTING_KEY,
                    successEvent
            );
            log.info("Payment success event published for order: {}", orderId);
        } else {
            PaymentReservationFailedEvent failureEvent = PaymentReservationFailedEvent.builder()
                    .sagaId("unknown-from-payment")
                    .orderId(orderId)
                    .success(false)
                    .reason("Payment failed at frontend")
                    .build();
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EVENT_EXCHANGE,
                    RabbitMQConfig.PAYMENT_FAILED_ROUTING_KEY,
                    failureEvent
            );
            log.warn("Payment failure event published for order: {}", orderId);
        }
    }
}

