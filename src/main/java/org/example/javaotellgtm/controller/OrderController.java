package org.example.javaotellgtm.controller;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javaotellgtm.dto.CreateOrderRequest;
import org.example.javaotellgtm.model.Order;
import org.example.javaotellgtm.model.OrderStatus;
import org.example.javaotellgtm.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Observed(
        name = "http.server.requests",
        contextualName = "create-order-endpoint",
        lowCardinalityKeyValues = {"http.method", "POST", "endpoint", "/api/orders"}
    )
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Received request to create order for customer: {}", request.getCustomerName());
        Order order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{orderId}")
    @Observed(
        name = "http.server.requests",
        contextualName = "get-order-endpoint",
        lowCardinalityKeyValues = {"http.method", "GET", "endpoint", "/api/orders/{id}"}
    )
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        log.info("Received request to get order: {}", orderId);
        Order order = orderService.getOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    @Observed(
        name = "http.server.requests",
        contextualName = "get-all-orders-endpoint",
        lowCardinalityKeyValues = {"http.method", "GET", "endpoint", "/api/orders"}
    )
    public ResponseEntity<List<Order>> getAllOrders() {
        log.info("Received request to get all orders");
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/customer/{customerId}")
    @Observed(
        name = "http.server.requests",
        contextualName = "get-orders-by-customer-endpoint",
        lowCardinalityKeyValues = {"http.method", "GET", "endpoint", "/api/orders/customer/{id}"}
    )
    public ResponseEntity<List<Order>> getOrdersByCustomer(@PathVariable String customerId) {
        log.info("Received request to get orders for customer: {}", customerId);
        List<Order> orders = orderService.getOrdersByCustomerId(customerId);
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{orderId}/status")
    @Observed(
        name = "http.server.requests",
        contextualName = "update-order-status-endpoint",
        lowCardinalityKeyValues = {"http.method", "PATCH", "endpoint", "/api/orders/{id}/status"}
    )
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, String> statusUpdate) {
        log.info("Received request to update status for order: {}", orderId);

        OrderStatus newStatus = OrderStatus.valueOf(statusUpdate.get("status"));
        Order order = orderService.updateOrderStatus(orderId, newStatus);

        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/cancel")
    @Observed(
        name = "http.server.requests",
        contextualName = "cancel-order-endpoint",
        lowCardinalityKeyValues = {"http.method", "POST", "endpoint", "/api/orders/{id}/cancel"}
    )
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderId) {
        log.info("Received request to cancel order: {}", orderId);
        orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "order-service"
        ));
    }
}
