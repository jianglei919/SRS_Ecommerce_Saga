package com.uwindsor.ecommerce.inventory.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for Inventory Service
 * Configures queues and exchanges for saga event communication
 */
@Configuration
public class RabbitMQConfig {

    // Queue names
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String INVENTORY_RESERVED_QUEUE = "inventory.reserved.queue";
    public static final String INVENTORY_FAILED_QUEUE = "inventory.failed.queue";

    // Exchange name
    public static final String EVENT_EXCHANGE = "saga.events";

    // Routing keys
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String INVENTORY_RESERVED_ROUTING_KEY = "inventory.reserved";
    public static final String INVENTORY_FAILED_ROUTING_KEY = "inventory.failed";

    /**
     * Order Created Queue - Consumed by Inventory Service from Order Service
     */
    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE, true);
    }

    /**
     * Inventory Reserved Queue - Published by Inventory Service
     */
    @Bean
    public Queue inventoryReservedQueue() {
        return new Queue(INVENTORY_RESERVED_QUEUE, true);
    }

    /**
     * Inventory Failed Queue - Published by Inventory Service
     */
    @Bean
    public Queue inventoryFailedQueue() {
        return new Queue(INVENTORY_FAILED_QUEUE, true);
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
     * Binding: Inventory Reserved Event
     */
    @Bean
    public Binding inventoryReservedBinding() {
        return BindingBuilder.bind(inventoryReservedQueue())
                .to(eventExchange())
                .with(INVENTORY_RESERVED_ROUTING_KEY);
    }

    /**
     * Binding: Inventory Failed Event
     */
    @Bean
    public Binding inventoryFailedBinding() {
        return BindingBuilder.bind(inventoryFailedQueue())
                .to(eventExchange())
                .with(INVENTORY_FAILED_ROUTING_KEY);
    }

    /**
     * Configure Jackson for JSON message serialization
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
