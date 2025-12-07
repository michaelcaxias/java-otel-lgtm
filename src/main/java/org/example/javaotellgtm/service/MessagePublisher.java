package org.example.javaotellgtm.service;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javaotellgtm.config.RabbitMQConfig;
import org.example.javaotellgtm.dto.OrderEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @Observed(
        name = "message.publish.order-event",
        contextualName = "publish-order-event",
        lowCardinalityKeyValues = {"message.type", "order-event", "destination", "rabbitmq"}
    )
    public void publishOrderEvent(OrderEvent event) {
        String routingKey = determineRoutingKey(event.getEventType());

        log.info("Publishing event {} for order {} with routing key {}",
                event.getEventType(), event.getOrderId(), routingKey);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                routingKey,
                event
        );

        log.info("Event published successfully");
    }

    @Observed(
        name = "message.publish.notification",
        contextualName = "publish-notification",
        lowCardinalityKeyValues = {"message.type", "notification", "destination", "rabbitmq"}
    )
    public void publishNotification(String email, String subject, String message) {
        log.info("Publishing notification to email: {}", email);

        var notification = new EmailNotification(email, subject, message);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_EMAIL_KEY,
                notification
        );

        log.info("Notification published successfully");
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
