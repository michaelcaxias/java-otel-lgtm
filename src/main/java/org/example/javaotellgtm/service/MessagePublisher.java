package org.example.javaotellgtm.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javaotellgtm.aop.SpanAttribute;
import org.example.javaotellgtm.aop.Traced;
import org.example.javaotellgtm.config.RabbitMQConfig;
import org.example.javaotellgtm.dto.OrderEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @Traced(value = "publish-order-event", kind = SpanKind.PRODUCER,
            attributes = {"messaging.system:rabbitmq", "messaging.destination_kind:exchange"})
    public void publishOrderEvent(OrderEvent event) {
        Span span = Span.current();
        SpanContext spanContext = span.getSpanContext();

        // ✅ Capturar contexto do span atual para criar link no consumer
        event.setTraceId(spanContext.getTraceId());
        event.setSpanId(spanContext.getSpanId());
        event.setTraceFlags(spanContext.getTraceFlags().asHex());

        span.setAttribute("messaging.destination", RabbitMQConfig.ORDER_EXCHANGE);
        span.setAttribute("event.type", event.getEventType().name());
        span.setAttribute("order.id", event.getOrderId());
        span.setAttribute("customer.id", event.getCustomerId());

        String routingKey = determineRoutingKey(event.getEventType());
        span.setAttribute("messaging.routing_key", routingKey);

        log.info("Publishing event {} for order {} with routing key {} (traceId: {}, spanId: {})",
                event.getEventType(), event.getOrderId(), routingKey,
                event.getTraceId(), event.getSpanId());

        span.addEvent("Publishing message to RabbitMQ with trace context");

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                routingKey,
                event
        );

        span.addEvent("Message published successfully");
        log.info("Event published successfully with trace link");
    }

    @Traced(value = "publish-notification", kind = SpanKind.PRODUCER,
            attributes = {"messaging.system:rabbitmq", "messaging.destination_kind:exchange"})
    public void publishNotification(
            @SpanAttribute("notification.email") String email,
            @SpanAttribute("notification.subject") String subject,
            String message) {

        Span span = Span.current();
        span.setAttribute("messaging.destination", RabbitMQConfig.NOTIFICATION_EXCHANGE);
        span.setAttribute("messaging.routing_key", RabbitMQConfig.NOTIFICATION_EMAIL_KEY);

        log.info("Publishing notification to email: {}", email);
        span.addEvent("Creating notification message");

        var notification = new EmailNotification(email, subject, message);

        span.addEvent("Publishing notification to RabbitMQ");

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_EMAIL_KEY,
                notification
        );

        span.addEvent("Notification published successfully");
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
