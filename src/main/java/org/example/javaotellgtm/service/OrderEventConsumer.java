package org.example.javaotellgtm.service;

import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
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
@Observed(name = "order.event.consumer")
public class OrderEventConsumer {

    private final Tracer tracer;
    private final MessagePublisher messagePublisher;

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void handleOrderCreated(OrderEvent event) {
        var span = tracer.nextSpan().name("handle-order-created");

        try (var ws = tracer.withSpan(span.start())) {
            span.tag("order.id", event.getOrderId());
            span.tag("event.type", event.getEventType().name());

            log.info("Processing ORDER_CREATED event for order: {}", event.getOrderId());

            // Simulate processing time
            simulateProcessing(500);

            // Send notification
            messagePublisher.publishNotification(
                    event.getCustomerEmail(),
                    "Order Confirmation",
                    String.format("Your order %s has been received! Total: $%.2f",
                            event.getOrderId(), event.getTotalAmount())
            );

            log.info("Order created event processed successfully");
        } catch (Exception e) {
            span.error(e);
            log.error("Error processing order created event", e);
        } finally {
            span.end();
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void handlePaymentEvent(OrderEvent event) {
        var span = tracer.nextSpan().name("handle-payment-event");

        try (var ws = tracer.withSpan(span.start())) {
            span.tag("order.id", event.getOrderId());
            span.tag("event.type", event.getEventType().name());
            span.tag("payment.amount", event.getTotalAmount().toString());

            log.info("Processing payment event {} for order: {}",
                    event.getEventType(), event.getOrderId());

            // Simulate payment processing
            simulateProcessing(1000);

            if (event.getEventType() == OrderEvent.EventType.PAYMENT_PROCESSING) {
                log.info("Payment processing started for order: {}", event.getOrderId());

                // Simulate payment validation
                boolean paymentSuccess = Math.random() > 0.1; // 90% success rate

                if (paymentSuccess) {
                    log.info("Payment confirmed for order: {}", event.getOrderId());
                    span.tag("payment.status", "confirmed");
                } else {
                    log.warn("Payment failed for order: {}", event.getOrderId());
                    span.tag("payment.status", "failed");
                }
            }

            log.info("Payment event processed successfully");
        } catch (Exception e) {
            span.error(e);
            log.error("Error processing payment event", e);
        } finally {
            span.end();
        }
    }

    @RabbitListener(queues = RabbitMQConfig.SHIPPING_QUEUE)
    public void handleShippingEvent(OrderEvent event) {
        var span = tracer.nextSpan().name("handle-shipping-event");

        try (var ws = tracer.withSpan(span.start())) {
            span.tag("order.id", event.getOrderId());
            span.tag("event.type", event.getEventType().name());

            log.info("Processing shipping event for order: {}", event.getOrderId());

            // Simulate shipping label generation
            simulateProcessing(700);

            String trackingNumber = generateTrackingNumber();
            span.tag("tracking.number", trackingNumber);

            log.info("Shipping label generated for order: {} - Tracking: {}",
                    event.getOrderId(), trackingNumber);

            // Send shipping notification
            messagePublisher.publishNotification(
                    event.getCustomerEmail(),
                    "Order Shipped",
                    String.format("Your order %s has been shipped! Tracking number: %s",
                            event.getOrderId(), trackingNumber)
            );

            log.info("Shipping event processed successfully");
        } catch (Exception e) {
            span.error(e);
            log.error("Error processing shipping event", e);
        } finally {
            span.end();
        }
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotification(MessagePublisher.EmailNotification notification) {
        var span = tracer.nextSpan().name("handle-notification");

        try (var ws = tracer.withSpan(span.start())) {
            span.tag("notification.email", notification.email());
            span.tag("notification.subject", notification.subject());

            log.info("Sending email to: {} - Subject: {}",
                    notification.email(), notification.subject());

            // Simulate email sending
            simulateProcessing(300);

            log.info("Email sent successfully to: {}", notification.email());
        } catch (Exception e) {
            span.error(e);
            log.error("Error sending notification", e);
        } finally {
            span.end();
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
