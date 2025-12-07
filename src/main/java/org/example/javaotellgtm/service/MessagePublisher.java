package org.example.javaotellgtm.service;

import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javaotellgtm.config.RabbitMQConfig;
import org.example.javaotellgtm.dto.OrderEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Observed(name = "message.publisher")
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final Tracer tracer;

    public void publishOrderEvent(OrderEvent event) {
        var span = tracer.nextSpan().name("publish-order-event");

        try (var ws = tracer.withSpan(span.start())) {
            span.tag("event.type", event.getEventType().name());
            span.tag("order.id", event.getOrderId());
            span.tag("customer.id", event.getCustomerId());

            String routingKey = determineRoutingKey(event.getEventType());

            log.info("Publishing event {} for order {} with routing key {}",
                    event.getEventType(), event.getOrderId(), routingKey);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    routingKey,
                    event
            );

            log.info("Event published successfully");
        } finally {
            span.end();
        }
    }

    public void publishNotification(String email, String subject, String message) {
        var span = tracer.nextSpan().name("publish-notification");

        try (var ws = tracer.withSpan(span.start())) {
            span.tag("notification.email", email);
            span.tag("notification.subject", subject);

            log.info("Publishing notification to email: {}", email);

            var notification = new EmailNotification(email, subject, message);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_EMAIL_KEY,
                    notification
            );

            log.info("Notification published successfully");
        } finally {
            span.end();
        }
    }

    private String determineRoutingKey(OrderEvent.EventType eventType) {
        return switch (eventType) {
            case ORDER_CREATED -> RabbitMQConfig.ORDER_CREATED_KEY;
            case PAYMENT_PROCESSING -> RabbitMQConfig.PAYMENT_PROCESSING_KEY;
            case PAYMENT_CONFIRMED -> RabbitMQConfig.PAYMENT_CONFIRMED_KEY;
            case ORDER_SHIPPED -> RabbitMQConfig.ORDER_SHIPPED_KEY;
            default -> RabbitMQConfig.ORDER_CREATED_KEY;
        };
    }

    public record EmailNotification(String email, String subject, String message) {}
}
