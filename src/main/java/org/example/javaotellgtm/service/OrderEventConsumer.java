package org.example.javaotellgtm.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javaotellgtm.config.RabbitMQConfig;
import org.example.javaotellgtm.dto.OrderEvent;
import org.example.javaotellgtm.traces.annotation.SpanAttribute;
import org.example.javaotellgtm.traces.annotation.TraceSpan;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final MessagePublisher messagePublisher;

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    @TraceSpan(value = "handle-order-created", kind = SpanKind.CONSUMER)
    public void handleOrderCreated(@SpanAttribute OrderEvent event) {
        log.info("Processing ORDER_CREATED event for order: {}", event.getOrderId());

        simulateProcessing(500);

        messagePublisher.publishNotification(
                event.getCustomerEmail(),
                "Order Confirmation",
                String.format("Your order %s has been received! Total: $%.2f",
                        event.getOrderId(), event.getTotalAmount())
        );

        log.info("Order created event processed successfully");
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    @TraceSpan(value = "handle-payment-event", kind = SpanKind.CONSUMER)
    public void handlePaymentEvent(OrderEvent event) {
        log.info("Processing payment event {} for order: {}",
                event.getEventType(), event.getOrderId());

        simulateProcessing(1000);

        if (event.getEventType() == OrderEvent.EventType.PAYMENT_PROCESSING) {
            log.info("Payment processing started for order: {}", event.getOrderId());

            // Simulate payment validation
            boolean paymentSuccess = Math.random() > 0.1; // 90% success rate

            if (paymentSuccess) {
                log.info("Payment confirmed for order: {}", event.getOrderId());
            } else {
                log.warn("Payment failed for order: {}", event.getOrderId());
            }
        }

        log.info("Payment event processed successfully");
    }

    @RabbitListener(queues = RabbitMQConfig.SHIPPING_QUEUE)
    @TraceSpan(value = "handle-shipping-event", kind = SpanKind.CONSUMER)
    public void handleShippingEvent(OrderEvent event) {
        Span span = Span.current();
        span.setAttribute("messaging.source", RabbitMQConfig.SHIPPING_QUEUE);
        span.setAttribute("order.id", event.getOrderId());
        span.setAttribute("event.type", event.getEventType().name());

        log.info("Processing shipping event for order: {}", event.getOrderId());
        span.addEvent("Starting shipping event processing");

        // Simulate shipping label generation
        span.addEvent("Generating shipping label");
        simulateProcessing(700);
        span.addEvent("Shipping label generation completed");

        String trackingNumber = generateTrackingNumber();
        span.setAttribute("tracking.number", trackingNumber);
        span.addEvent("Tracking number generated");

        log.info("Shipping label generated for order: {} - Tracking: {}",
                event.getOrderId(), trackingNumber);

        // Send shipping notification
        span.addEvent("Sending shipping notification");
        messagePublisher.publishNotification(
                event.getCustomerEmail(),
                "Order Shipped",
                String.format("Your order %s has been shipped! Tracking number: %s",
                        event.getOrderId(), trackingNumber)
        );
        span.addEvent("Shipping notification sent");

        log.info("Shipping event processed successfully");
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    @TraceSpan(value = "handle-notification", kind = SpanKind.CONSUMER)
    public void handleNotification(MessagePublisher.EmailNotification notification) {
        Span span = Span.current();
        span.setAttribute("messaging.source", RabbitMQConfig.NOTIFICATION_QUEUE);
        span.setAttribute("notification.email", notification.email());
        span.setAttribute("notification.subject", notification.subject());

        log.info("Sending email to: {} - Subject: {}",
                notification.email(), notification.subject());
        span.addEvent("Starting email notification");

        // Simulate email sending
        span.addEvent("Sending email via SMTP");
        simulateProcessing(300);
        span.addEvent("Email sent via SMTP");

        span.setAttribute("email.sent", "true");
        log.info("Email sent successfully to: {}", notification.email());
    }

    private void simulateProcessing(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String generateTrackingNumber() {
        return "TRK" + System.currentTimeMillis() +
                (int)(Math.random() * 1000);
    }
}
