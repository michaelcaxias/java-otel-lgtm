package org.example.javaotellgtm.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javaotellgtm.config.RabbitMQConfig;
import org.example.javaotellgtm.dto.OrderEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final MessagePublisher messagePublisher;

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    @WithSpan(value = "handle-order-created", kind = SpanKind.CONSUMER)
    public void handleOrderCreated(OrderEvent event) {
        Span span = Span.current();
        span.setAttribute("messaging.system", "rabbitmq");
        span.setAttribute("messaging.source", RabbitMQConfig.ORDER_QUEUE);
        span.setAttribute("messaging.operation", "process");
        span.setAttribute("order.id", event.getOrderId());
        span.setAttribute("event.type", event.getEventType().name());
        span.setAttribute("customer.email", event.getCustomerEmail());

        log.info("Processing ORDER_CREATED event for order: {}", event.getOrderId());
        span.addEvent("Starting order created event processing");

        try {
            // Simulate processing time
            span.addEvent("Simulating order processing");
            simulateProcessing(500);
            span.addEvent("Order processing simulation completed");

            // Send notification
            span.addEvent("Sending order confirmation notification");
            messagePublisher.publishNotification(
                    event.getCustomerEmail(),
                    "Order Confirmation",
                    String.format("Your order %s has been received! Total: $%.2f",
                            event.getOrderId(), event.getTotalAmount())
            );
            span.addEvent("Notification sent successfully");

            log.info("Order created event processed successfully");
        } catch (Exception e) {
            span.recordException(e);
            span.setAttribute("error", "true");
            span.addEvent("Error processing order created event");
            log.error("Error processing order created event", e);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    @WithSpan(value = "handle-payment-event", kind = SpanKind.CONSUMER)
    public void handlePaymentEvent(OrderEvent event) {
        Span span = Span.current();
        span.setAttribute("messaging.system", "rabbitmq");
        span.setAttribute("messaging.source", RabbitMQConfig.PAYMENT_QUEUE);
        span.setAttribute("messaging.operation", "process");
        span.setAttribute("order.id", event.getOrderId());
        span.setAttribute("event.type", event.getEventType().name());
        span.setAttribute("payment.amount", event.getTotalAmount().toString());

        log.info("Processing payment event {} for order: {}",
                event.getEventType(), event.getOrderId());
        span.addEvent("Starting payment event processing");

        try {
            // Simulate payment processing
            span.addEvent("Simulating payment processing");
            simulateProcessing(1000);
            span.addEvent("Payment processing simulation completed");

            if (event.getEventType() == OrderEvent.EventType.PAYMENT_PROCESSING) {
                log.info("Payment processing started for order: {}", event.getOrderId());
                span.addEvent("Payment processing started");

                // Simulate payment validation
                boolean paymentSuccess = Math.random() > 0.1; // 90% success rate

                if (paymentSuccess) {
                    span.setAttribute("payment.status", "confirmed");
                    span.addEvent("Payment confirmed");
                    log.info("Payment confirmed for order: {}", event.getOrderId());
                } else {
                    span.setAttribute("payment.status", "failed");
                    span.addEvent("Payment failed");
                    log.warn("Payment failed for order: {}", event.getOrderId());
                }
            }

            span.addEvent("Payment event processing completed");
            log.info("Payment event processed successfully");
        } catch (Exception e) {
            span.recordException(e);
            span.setAttribute("error", "true");
            span.addEvent("Error processing payment event");
            log.error("Error processing payment event", e);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.SHIPPING_QUEUE)
    @WithSpan(value = "handle-shipping-event", kind = SpanKind.CONSUMER)
    public void handleShippingEvent(OrderEvent event) {
        Span span = Span.current();
        span.setAttribute("messaging.system", "rabbitmq");
        span.setAttribute("messaging.source", RabbitMQConfig.SHIPPING_QUEUE);
        span.setAttribute("messaging.operation", "process");
        span.setAttribute("order.id", event.getOrderId());
        span.setAttribute("event.type", event.getEventType().name());

        log.info("Processing shipping event for order: {}", event.getOrderId());
        span.addEvent("Starting shipping event processing");

        try {
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
        } catch (Exception e) {
            span.recordException(e);
            span.setAttribute("error", "true");
            span.addEvent("Error processing shipping event");
            log.error("Error processing shipping event", e);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    @WithSpan(value = "handle-notification", kind = SpanKind.CONSUMER)
    public void handleNotification(MessagePublisher.EmailNotification notification) {
        Span span = Span.current();
        span.setAttribute("messaging.system", "rabbitmq");
        span.setAttribute("messaging.source", RabbitMQConfig.NOTIFICATION_QUEUE);
        span.setAttribute("messaging.operation", "process");
        span.setAttribute("notification.email", notification.email());
        span.setAttribute("notification.subject", notification.subject());

        log.info("Sending email to: {} - Subject: {}",
                notification.email(), notification.subject());
        span.addEvent("Starting email notification");

        try {
            // Simulate email sending
            span.addEvent("Sending email via SMTP");
            simulateProcessing(300);
            span.addEvent("Email sent via SMTP");

            span.setAttribute("email.sent", "true");
            log.info("Email sent successfully to: {}", notification.email());
        } catch (Exception e) {
            span.recordException(e);
            span.setAttribute("error", "true");
            span.setAttribute("email.sent", "false");
            span.addEvent("Error sending notification");
            log.error("Error sending notification", e);
            throw e;
        }
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
