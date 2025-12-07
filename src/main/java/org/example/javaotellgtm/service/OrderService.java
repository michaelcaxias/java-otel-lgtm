package org.example.javaotellgtm.service;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javaotellgtm.dto.CreateOrderRequest;
import org.example.javaotellgtm.dto.OrderEvent;
import org.example.javaotellgtm.model.Order;
import org.example.javaotellgtm.model.OrderStatus;
import org.example.javaotellgtm.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MessagePublisher messagePublisher;

    @Observed(
        name = "order.create",
        contextualName = "create-order",
        lowCardinalityKeyValues = {"operation", "create"}
    )
    public Order createOrder(CreateOrderRequest request) {
        log.info("Creating new order for customer: {}", request.getCustomerName());

        // Convert request items to order items and calculate total
        List<Order.OrderItem> orderItems = request.getItems().stream()
                .map(item -> {
                    BigDecimal subtotal = item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
                    return Order.OrderItem.builder()
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .subtotal(subtotal)
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalAmount = orderItems.stream()
                .map(Order.OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create order
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .items(orderItems)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .shippingAddress(request.getShippingAddress())
                .paymentMethod(request.getPaymentMethod())
                .build();

        order = orderRepository.save(order);

        log.info("Order created successfully with ID: {}", order.getId());

        // Publish order created event
        publishOrderEvent(order, OrderEvent.EventType.ORDER_CREATED);

        return order;
    }

    @Observed(
        name = "order.get",
        contextualName = "get-order",
        lowCardinalityKeyValues = {"operation", "read"}
    )
    public Order getOrder(String orderId) {
        log.info("Fetching order: {}", orderId);
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    @Observed(
        name = "order.list.all",
        contextualName = "list-all-orders",
        lowCardinalityKeyValues = {"operation", "read"}
    )
    public List<Order> getAllOrders() {
        log.info("Fetching all orders");
        return orderRepository.findAll();
    }

    @Observed(
        name = "order.list.by-customer",
        contextualName = "list-orders-by-customer",
        lowCardinalityKeyValues = {"operation", "read"}
    )
    public List<Order> getOrdersByCustomerId(String customerId) {
        log.info("Fetching orders for customer: {}", customerId);
        return orderRepository.findByCustomerId(customerId);
    }

    @Observed(
        name = "order.update-status",
        contextualName = "update-order-status",
        lowCardinalityKeyValues = {"operation", "update"}
    )
    public Order updateOrderStatus(String orderId, OrderStatus newStatus) {
        log.info("Updating order {} status to {}", orderId, newStatus);

        Order order = getOrder(orderId);
        OrderStatus oldStatus = order.getStatus();

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        log.info("Order {} status updated from {} to {}", orderId, oldStatus, newStatus);

        // Publish appropriate event based on new status
        OrderEvent.EventType eventType = mapStatusToEventType(newStatus);
        publishOrderEvent(order, eventType);

        return order;
    }

    @Observed(
        name = "order.cancel",
        contextualName = "cancel-order",
        lowCardinalityKeyValues = {"operation", "cancel"}
    )
    public void cancelOrder(String orderId) {
        log.info("Cancelling order: {}", orderId);
        updateOrderStatus(orderId, OrderStatus.CANCELLED);
    }

    private void publishOrderEvent(Order order, OrderEvent.EventType eventType) {
        OrderEvent event = OrderEvent.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .customerEmail(order.getCustomerEmail())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .build();

        messagePublisher.publishOrderEvent(event);
    }

    private OrderEvent.EventType mapStatusToEventType(OrderStatus status) {
        return switch (status) {
            case PENDING -> OrderEvent.EventType.ORDER_CREATED;
            case PAYMENT_PROCESSING -> OrderEvent.EventType.PAYMENT_PROCESSING;
            case PAYMENT_CONFIRMED -> OrderEvent.EventType.PAYMENT_CONFIRMED;
            case PREPARING -> OrderEvent.EventType.ORDER_PREPARING;
            case SHIPPED -> OrderEvent.EventType.ORDER_SHIPPED;
            case DELIVERED -> OrderEvent.EventType.ORDER_DELIVERED;
            case CANCELLED -> OrderEvent.EventType.ORDER_CANCELLED;
        };
    }
}
