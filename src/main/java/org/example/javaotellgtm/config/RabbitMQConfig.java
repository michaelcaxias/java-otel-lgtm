package org.example.javaotellgtm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitMQConfig {

    // Queue names
    public static final String ORDER_QUEUE = "order.queue";
    public static final String PAYMENT_QUEUE = "payment.queue";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String SHIPPING_QUEUE = "shipping.queue";

    // Exchange names
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";

    // Routing keys
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String PAYMENT_PROCESSING_KEY = "payment.processing";
    public static final String PAYMENT_CONFIRMED_KEY = "payment.confirmed";
    public static final String ORDER_SHIPPED_KEY = "order.shipped";
    public static final String NOTIFICATION_EMAIL_KEY = "notification.email";

    // Order Queue and Exchange
    @Bean
    public Queue orderQueue() {
        return new Queue(ORDER_QUEUE, true);
    }

    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Queue shippingQueue() {
        return new Queue(SHIPPING_QUEUE, true);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    // Bindings
    @Bean
    public Binding orderBinding() {
        return BindingBuilder
                .bind(orderQueue())
                .to(orderExchange())
                .with(ORDER_CREATED_KEY);
    }

    @Bean
    public Binding paymentProcessingBinding() {
        return BindingBuilder
                .bind(paymentQueue())
                .to(orderExchange())
                .with(PAYMENT_PROCESSING_KEY);
    }

    @Bean
    public Binding paymentConfirmedBinding() {
        return BindingBuilder
                .bind(paymentQueue())
                .to(orderExchange())
                .with(PAYMENT_CONFIRMED_KEY);
    }

    @Bean
    public Binding shippingBinding() {
        return BindingBuilder
                .bind(shippingQueue())
                .to(orderExchange())
                .with(ORDER_SHIPPED_KEY);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_EMAIL_KEY);
    }

    // Message converter
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                        MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
