package org.example.javaotellgtm.service;

import io.micrometer.observation.annotation.Observed;
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
    @Observed(
        name = "message.consume.order-created",
        contextualName = "handle-order-created",
        lowCardinalityKeyValues = {"message.type", "order-created", "source", "rabbitmq"}
    )
    public void handleOrderCreated(OrderEvent event) {
        log.info("Processing ORDER_CREATED event for order: {}", event.getOrderId());

        try {
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
            log.error("Error processing order created event", e);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    @Observed(
        name = "message.consume.payment-event",
        contextualName = "handle-payment-event",
        lowCardinalityKeyValues = {"message.type", "payment-event", "source", "rabbitmq"}
    )
    public void handlePaymentEvent(OrderEvent event) {
        log.info("Processing payment event {} for order: {}",
                event.getEventType(), event.getOrderId());

        try {
            // Simulate payment processing
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
        } catch (Exception e) {
            log.error("Error processing payment event", e);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.SHIPPING_QUEUE)
    @Observed(
        name = "message.consume.shipping-event",
        contextualName = "handle-shipping-event",
        lowCardinalityKeyValues = {"message.type", "shipping-event", "source", "rabbitmq"}
    )
    public void handleShippingEvent(OrderEvent event) {
        log.info("Processing shipping event for order: {}", event.getOrderId());

        try {
            // Simulate shipping label generation
            simulateProcessing(700);

            String trackingNumber = generateTrackingNumber();

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
            log.error("Error processing shipping event", e);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    @Observed(
        name = "message.consume.notification",
        contextualName = "handle-notification",
        lowCardinalityKeyValues = {"message.type", "notification", "source", "rabbitmq"}
    )
    public void handleNotification(MessagePublisher.EmailNotification notification) {
        log.info("Sending email to: {} - Subject: {}",
                notification.email(), notification.subject());

        try {
            // Simulate email sending
            simulateProcessing(300);

            log.info("Email sent successfully to: {}", notification.email());
        } catch (Exception e) {
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
