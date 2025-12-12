package org.example.javaotellgtm.service;

import io.opentelemetry.api.trace.Span;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javaotellgtm.dto.CreateOrderRequest;
import org.example.javaotellgtm.dto.OrderEvent;
import org.example.javaotellgtm.model.Order;
import org.example.javaotellgtm.model.OrderStatus;
import org.example.javaotellgtm.repository.OrderRepository;
import org.example.javaotellgtm.traces.annotation.SpanAttribute;
import org.example.javaotellgtm.traces.annotation.TraceSpan;
import org.example.javaotellgtm.traces.constants.AttributeName;
import org.example.javaotellgtm.traces.constants.SpanName;
import org.example.javaotellgtm.traces.processor.SpanWrap;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MessagePublisher messagePublisher;

    /**
     * Creates a new order.
     * Note: Email is NOT added as span attribute as it's PII (Personally Identifiable Information).
     */
    @TraceSpan(SpanName.ORDER_CREATE)
    public Order createOrder(
            @SpanAttribute("customer.id") String customerId,
            @SpanAttribute("customer.name") String customerName,
            CreateOrderRequest request) {

        Span span = Span.current();
        log.info("Creating new order for customer: {}", customerName);
        span.addEvent("Starting order creation");

        // Convert request items to order items and calculate total
        span.addEvent("Calculating order items");

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

        // Add runtime attributes
        SpanWrap.addAttributes(Map.of(
                AttributeName.ORDER_ITEMS_COUNT.getKey(), String.valueOf(orderItems.size()),
                AttributeName.ORDER_TOTAL_AMOUNT.getKey(), totalAmount.toString(),
                AttributeName.ORDER_PAYMENT_METHOD.getKey(), request.getPaymentMethod()
        ));
        span.addEvent("Order total calculated");

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

        span.addEvent("Saving order to database");
        order = orderRepository.save(order);

        // Add order attributes using TelemetryEvent
        SpanWrap.addAttributes(order);
        span.addEvent("Order saved to database");

        log.info("Order created successfully with ID: {}", order.getId());

        // Publish order created event
        span.addEvent("Publishing order created event");
        publishOrderEvent(order, OrderEvent.EventType.ORDER_CREATED);
        span.addEvent("Order event published");

        return order;
    }

    @TraceSpan(SpanName.ORDER_FETCH)
    public Order getOrder(@SpanAttribute("order.id") String orderId) {
        Span span = Span.current();

        log.info("Fetching order: {}", orderId);
        span.addEvent("Querying database for order");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    span.addEvent("Order not found");
                    SpanWrap.addAttributes(Map.of(
                            AttributeName.ERROR.getKey(), "true",
                            AttributeName.ERROR_MESSAGE.getKey(), "Order not found: " + orderId
                    ));
                    return new RuntimeException("Order not found: " + orderId);
                });

        // Add order attributes using TelemetryEvent
        SpanWrap.addAttributes(order);
        span.addEvent("Order retrieved successfully");
        return order;
    }

    @TraceSpan(SpanName.ORDER_LIST_ALL)
    public List<Order> getAllOrders() {
        Span span = Span.current();

        log.info("Fetching all orders");
        span.addEvent("Querying all orders from database");

        List<Order> orders = orderRepository.findAll();

        SpanWrap.addAttributes(Map.of(
                AttributeName.ORDERS_COUNT.getKey(), String.valueOf(orders.size())
        ));
        span.addEvent("Orders retrieved");

        return orders;
    }

    @TraceSpan(SpanName.ORDER_LIST_BY_CUSTOMER)
    public List<Order> getOrdersByCustomerId(@SpanAttribute("customer.id") String customerId) {
        Span span = Span.current();

        log.info("Fetching orders for customer: {}", customerId);
        span.addEvent("Querying orders by customer");

        List<Order> orders = orderRepository.findByCustomerId(customerId);

        SpanWrap.addAttributes(Map.of(
                AttributeName.ORDERS_COUNT.getKey(), String.valueOf(orders.size())
        ));
        span.addEvent("Customer orders retrieved");

        return orders;
    }

    @TraceSpan(SpanName.ORDER_UPDATE_STATUS)
    public Order updateOrderStatus(
            @SpanAttribute("order.id") String orderId,
            @SpanAttribute("order.status.new") OrderStatus newStatus) {

        Span span = Span.current();

        log.info("Updating order {} status to {}", orderId, newStatus);
        span.addEvent("Starting status update");

        Order order = getOrder(orderId);
        OrderStatus oldStatus = order.getStatus();

        SpanWrap.addAttributes(Map.of(
                AttributeName.ORDER_STATUS_OLD.getKey(), oldStatus.name()
        ));
        span.addEvent("Current status retrieved");

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        span.addEvent("Saving updated order");
        order = orderRepository.save(order);
        span.addEvent("Order status updated in database");

        log.info("Order {} status updated from {} to {}", orderId, oldStatus, newStatus);

        // Publish appropriate event based on new status
        OrderEvent.EventType eventType = mapStatusToEventType(newStatus);
        SpanWrap.addAttributes(Map.of(
                AttributeName.EVENT_TYPE.getKey(), eventType.name()
        ));
        span.addEvent("Publishing status change event");
        publishOrderEvent(order, eventType);
        span.addEvent("Status change event published");

        return order;
    }

    @TraceSpan(SpanName.ORDER_CANCEL)
    public void cancelOrder(@SpanAttribute("order.id") String orderId) {
        Span span = Span.current();

        log.info("Cancelling order: {}", orderId);
        span.addEvent("Initiating order cancellation");

        updateOrderStatus(orderId, OrderStatus.CANCELLED);

        span.addEvent("Order cancelled successfully");
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
