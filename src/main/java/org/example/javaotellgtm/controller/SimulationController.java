package org.example.javaotellgtm.controller;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javaotellgtm.dto.CreateOrderRequest;
import org.example.javaotellgtm.model.Order;
import org.example.javaotellgtm.model.OrderStatus;
import org.example.javaotellgtm.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final OrderService orderService;
    private final Random random = new Random();

    private static final String[] CUSTOMER_NAMES = {
            "João Silva", "Maria Santos", "Pedro Oliveira", "Ana Costa",
            "Carlos Souza", "Juliana Lima", "Rafael Alves", "Fernanda Rodrigues"
    };

    private static final String[] PRODUCTS = {
            "Notebook", "Mouse", "Teclado", "Monitor", "Webcam",
            "Headset", "SSD", "RAM", "GPU", "Mousepad"
    };

    @PostMapping("/create-sample-order")
    @Observed(
        name = "simulation.create-sample-order",
        contextualName = "create-sample-order",
        lowCardinalityKeyValues = {"operation", "simulation"}
    )
    public ResponseEntity<Order> createSampleOrder() {
        log.info("Creating sample order");

        String customerName = CUSTOMER_NAMES[random.nextInt(CUSTOMER_NAMES.length)];
        String customerId = "CUST-" + random.nextInt(10000);
        String customerEmail = customerName.toLowerCase().replace(" ", ".") + "@email.com";

        int itemCount = random.nextInt(3) + 1; // 1 to 3 items
        List<CreateOrderRequest.OrderItemRequest> items = new java.util.ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            String product = PRODUCTS[random.nextInt(PRODUCTS.length)];
            items.add(CreateOrderRequest.OrderItemRequest.builder()
                    .productId("PROD-" + random.nextInt(1000))
                    .productName(product)
                    .quantity(random.nextInt(3) + 1)
                    .unitPrice(BigDecimal.valueOf(random.nextDouble() * 1000 + 50))
                    .build());
        }

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(customerId)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .items(items)
                .shippingAddress(customerId + " Street, " + random.nextInt(1000))
                .paymentMethod("CREDIT_CARD")
                .build();

        Order order = orderService.createOrder(customerId, customerName, customerEmail, request);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/simulate-order-flow/{orderId}")
    @Observed(
        name = "simulation.order-flow",
        contextualName = "simulate-order-flow",
        lowCardinalityKeyValues = {"operation", "simulation"}
    )
    public ResponseEntity<Map<String, String>> simulateOrderFlow(@PathVariable String orderId) {
        log.info("Starting order flow simulation for order: {}", orderId);

        CompletableFuture.runAsync(() -> {
            try {
                // Payment Processing
                Thread.sleep(2000);
                orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_PROCESSING);
                log.info("Order {} - Payment processing", orderId);

                // Payment Confirmed
                Thread.sleep(3000);
                orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_CONFIRMED);
                log.info("Order {} - Payment confirmed", orderId);

                // Preparing
                Thread.sleep(2000);
                orderService.updateOrderStatus(orderId, OrderStatus.PREPARING);
                log.info("Order {} - Preparing", orderId);

                // Shipped
                Thread.sleep(4000);
                orderService.updateOrderStatus(orderId, OrderStatus.SHIPPED);
                log.info("Order {} - Shipped", orderId);

                // Delivered
                Thread.sleep(5000);
                orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED);
                log.info("Order {} - Delivered", orderId);

            } catch (Exception e) {
                log.error("Error simulating order flow", e);
            }
        });

        return ResponseEntity.ok(Map.of(
                "message", "Order flow simulation started",
                "orderId", orderId
        ));
    }

    @PostMapping("/generate-traffic")
    @Observed(
        name = "simulation.generate-traffic",
        contextualName = "generate-traffic",
        lowCardinalityKeyValues = {"operation", "simulation"}
    )
    public ResponseEntity<Map<String, Object>> generateTraffic(
            @RequestParam(defaultValue = "5") int orderCount) {
        log.info("Generating traffic with {} orders", orderCount);

        List<String> orderIds = new java.util.ArrayList<>();

        for (int i = 0; i < orderCount; i++) {
            try {
                Order order = createSampleOrder().getBody();
                if (order != null) {
                    orderIds.add(order.getId());

                    // Start flow simulation for some orders
                    if (random.nextBoolean()) {
                        simulateOrderFlow(order.getId());
                    }
                }

                // Random delay between orders
                Thread.sleep(random.nextInt(500) + 200);
            } catch (Exception e) {
                log.error("Error generating traffic", e);
            }
        }

        return ResponseEntity.ok(Map.of(
                "message", "Traffic generation completed",
                "ordersCreated", orderIds.size(),
                "orderIds", orderIds
        ));
    }
}
