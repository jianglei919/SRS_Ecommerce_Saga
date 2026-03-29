package com.uwindsor.ecommerce.payment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for Payment Service
 * Configures queues and exchanges for saga event communication
 */
@Configuration
public class RabbitMQConfig {

    // Queue names
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String PAYMENT_RESERVED_QUEUE = "payment.reserved.queue";
    public static final String PAYMENT_FAILED_QUEUE = "payment.failed.queue";

    // Exchange name
    public static final String EVENT_EXCHANGE = "saga.events";

    // Routing keys
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String PAYMENT_RESERVED_ROUTING_KEY = "payment.reserved";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";

    /**
     * Order Created Queue - Consumed by Payment Service from Order Service
     */
    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE, true);
    }

    /**
     * Payment Reserved Queue - Published by Payment Service
     */
    @Bean
    public Queue paymentReservedQueue() {
        return new Queue(PAYMENT_RESERVED_QUEUE, true);
    }

    /**
     * Payment Failed Queue - Published by Payment Service
     */
    @Bean
    public Queue paymentFailedQueue() {
        return new Queue(PAYMENT_FAILED_QUEUE, true);
    }

    /**
     * Topic Exchange for all saga events
     */
    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EVENT_EXCHANGE, true, false);
    }

    /**
     * Binding: Order Created Event
     */
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue())
                .to(eventExchange())
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    /**
     * Binding: Payment Reserved Event
     */
    @Bean
    public Binding paymentReservedBinding() {
        return BindingBuilder.bind(paymentReservedQueue())
                .to(eventExchange())
                .with(PAYMENT_RESERVED_ROUTING_KEY);
    }

    /**
     * Binding: Payment Failed Event
     */
    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue())
                .to(eventExchange())
                .with(PAYMENT_FAILED_ROUTING_KEY);
    }

    /**
     * Configure Jackson for JSON message serialization
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
